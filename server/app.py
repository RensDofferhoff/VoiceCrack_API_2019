import urllib3
import json
from urllib.parse import urlencode
from flask import Flask, request, make_response, abort
import tempfile
import random
import subprocess
import librosa
import numba
import sys
import sqlite3
import numpy as np

min_freq = 4000
max_freq = 14000
freq_bin_width = 300

API_KEY='8f0d6731959f4aed92dfc4331130e4b0'
ENTRY_POINT='https://asv.cognitiveservices.azure.com/spid/v1.0'
http = urllib3.PoolManager()

conn = sqlite3.connect('/var/www/app/content/app.db', check_same_thread = False)
c = conn.cursor()
c.execute('CREATE TABLE IF NOT EXISTS users (user_id INTEGER PRIMARY KEY AUTOINCREMENT, username text UNIQUE, ms_id text, enrolled INTEGER);')
c.execute('CREATE TABLE IF NOT EXISTS tones (user_id INTEGER, seq_num INTEGER, freq INTEGER, used INTEGER, PRIMARY KEY(user_id, seq_num));')
conn.commit()

app = Flask(__name__)


@app.route('/register/<username>')
def register(username):
    #inserting new username auto checks if unique
    query = 'INSERT INTO users (username, enrolled) VALUES (?,0);'
    try:
        c.execute(query, (username,))
        conn.commit()
    except Exception as err:
        print('Query Failed: %s\nError: %s' % (query, str(err)))
        return str(2)
    
    header = {'Content-Type': 'application/json',
              'Ocp-Apim-Subscription-Key': API_KEY}
    reg_body = '{"locale":"en-us",}'
    resp =  http.request('POST', ENTRY_POINT + '/verificationProfiles', body=reg_body, headers=header) 
    status = int(resp.status)
    if status != 200:
        return str(1)
    response =  json.loads(resp.data)
    ms_id = response['verificationProfileId']
    
    query = 'UPDATE users SET ms_id = ? WHERE username = ?'
    try:
        c.execute(query, (ms_id, username,))
        conn.commit()
    except Exception as err:
        print('failed to insert new ms_id')
        return str(-1)
    return str(0)
        
    

@app.route('/enroll/<username>', methods=['POST'])
def enroll(username):
    ms_id, error = get_ms_id(username)
    if len(request.files) != 0:
        audio_data = audio_ms_format_conversion(request.files[list(request.files.keys())[0]])
        fields= {'BinaryData': audio_data}
        header = {'Content-Type': 'application/octet-stream',
                'Ocp-Apim-Subscription-Key': API_KEY}
    
        resp = http.request('POST', ENTRY_POINT + '/verificationProfiles/' + ms_id + '/enroll', body=audio_data, headers=header)
        return str(resp.data)
    return str(1)

@app.route('/login/<username>')
def request_login(username):
    with tempfile.TemporaryDirectory() as tmpdirname:
        #get previously used tones
        user_id, error = get_user_id(username)
        if(error):
            print('failed finding this user')
            abort(404)
        tmp, previous_tones, error = get_tones(user_id)
        
        #choose tone
        possible_tones = [x for x in range(min_freq, max_freq, freq_bin_width) if x not in previous_tones]
        secure_random = random.SystemRandom()
        tone = secure_random.choice(possible_tones)
        
        #for safety mark all previous tones as used
        query = 'UPDATE tones SET used = 1 WHERE user_id = ?'
        try:
            c.execute(query, (user_id,))
            conn.commit()
        except Exception as err:
            print('failed set tone as used')
            return str(-1)
        
        #put tone into database
        query = 'INSERT INTO tones (user_id, seq_num, freq, used) VALUES (?,(SELECT COALESCE(MAX(seq_num),0) + 1 FROM tones WHERE user_id = ?),?, 0);'
        try:
            c.execute(query, (user_id, user_id, tone))
            conn.commit()
        except Exception as err:
            print('Query Failed: %s\nError: %s' % (query, str(err)))
            print('Failed insert new tone')
            return str(-1)
            
        #generate and send back tone
        out_path = create_tone(tone, tmpdirname)
        response = make_response(open(out_path, 'rb').read())
        response.headers['Content-Type'] = 'audio/wav'
        response.headers['Content-Disposition'] = 'attachment; filename=sound.wav'
        return response
    return str(-1)
    
@app.route('/authenticate/<username>', methods=['POST'])
def verify_rec(username):
    ms_id, error = get_ms_id(username)
    user_id, error = get_ms_id(username)
    if len(request.files) != 0:
        #get used tones and current tone
        user_id, error = get_user_id(username)
        if(error):
            print('failed finding this user')
            return str(-1)
        current_tone, previous_tones, error = get_tones(user_id)
        
        #pitch detection
        audio_file = request.files[list(request.files.keys())[0]]
        freqs = calc_pitches(audio_file)
        print(freqs)
        print(current_tone)
        print(previous_tones)
        #detect current tone
        if not current_tone in freqs:
            return str(2)
            
        #mark tone as used
        query = 'UPDATE tones SET used = 1 WHERE user_id = ? AND freq = ?'
        try:
            c.execute(query, (user_id, current_tone,))
            conn.commit()
        except Exception as err:
            print('failed set tone as used')
            return str(-1)
        #detect replay attack
        for freq in previous_tones:
            if freq in freqs:
                return str(3)
            
        #use MS-ASV to recognize speaker
        status, result = verify_ms(ms_id, audio_file)
        if status != 200:
            return str(1) 
        if result:
            return str(0)
        else:
            return str(1)
    return str(-1)
    


def get_ms_id(username):
    error = False
    query = 'SELECT ms_id FROM users WHERE username=?;'
    ms_id = ''
    try:
        c.execute(query, (username,))
        ms_id = c.fetchone()[0]
    except Exception as err:
        print('Query Failed: %s\nError: %s' % (query, str(err)))
        error = True
    return ms_id, error

def get_user_id(username):
    error = False
    query = 'SELECT user_id FROM users WHERE username=?;'
    user_id = -1
    try:
        c.execute(query, (username,))
        user_id = c.fetchone()[0]
    except Exception as err:
        print('Query Failed: %s\nError: %s' % (query, str(err)))
        error = True
    return user_id, error

def get_tones(user_id):
    query_previous = 'SELECT seq_num, freq, used FROM tones WHERE user_id=? AND used=1;'
    query_current = 'SELECT freq FROM tones WHERE user_id=? AND used=0;'
    previous_tones = []
    current_tone = -1
    try:
        c.execute(query_previous, (user_id,))
        previous_tones = [x[1] for x in sorted(c.fetchall(), key=lambda row: row[0]) if x[2] != 0]
        c.execute(query_current, (user_id,))
        row = c.fetchone()
        if row != None:
            current_tone = row[0] 
    except Exception as err:
        print('Error: %s' % ( str(err)))
        print('Tone Query Failed')
        return current_tone, [], True
    return current_tone, previous_tones, False


def verify_ms(ms_id, file):
    get_args = urlencode({'verificationProfileId' : ms_id})

    header = {'Content-Type': 'application/octet-stream',
              'Ocp-Apim-Subscription-Key': API_KEY}
    
    audio_data = audio_ms_format_conversion(file)
    resp =  http.request('POST', ENTRY_POINT + '/verify?' + get_args, body=audio_data, headers=header) 
    status = int(resp.status)
    
    response =  json.loads(resp.data)
    if status != 200:
        result = ''
    else:
        result = response['result']

    return status, result == 'Accept'


def audio_ms_format_conversion(audio_file):
    with tempfile.TemporaryDirectory() as tmpdirname:
        in_path = tmpdirname + '/in.mp3'
        out_path = tmpdirname + '/out.wav'
        in_file = open(in_path, 'wb')
        in_file.write(audio_file.read())
        audio_file.seek(0)
        subprocess.call(['ffmpeg', '-i', in_path, '-acodec', 'pcm_s16le', '-ac', '1', '-ar', '16000', out_path])
        out_file = open(out_path, 'rb')
        return out_file.read()

    
def create_tone(freq, temp_dir):
    temp_path = temp_dir + '/temp.wav'
    tone = librosa.tone(freq / 2 , duration=10)
    librosa.output.write_wav(temp_path, tone, 44100)
    out_path = temp_dir + '/out.wav'
    subprocess.call(['ffmpeg', '-i', temp_path , '-acodec', 'pcm_s16le', '-ac', '1', '-ar', '44100', out_path])
    return out_path


def calc_pitches(audio_file):
    with tempfile.TemporaryDirectory() as tmpdirname:
        #conversion to wav 44100 s16 mono        
        in_path = tmpdirname + '/in.mp3'
        out_path = tmpdirname + '/out.wav'
        in_file = open(in_path, 'wb')
        in_file.write(audio_file.read())
        audio_file.seek(0)
        subprocess.call(['ffmpeg', '-i', in_path, '-acodec', 'pcm_s16le', '-ac', '1', '-ar', '44100', out_path])
    
        #pitch detection
        y, sr = librosa.load(out_path, sr=44100)
        duration = librosa.get_duration(y=y, sr=sr)
        pitches, magnitudes = librosa.piptrack(y=y, sr=sr, fmin=min_freq, fmax=max_freq, hop_length=int(duration * sr / 10))
        #for i in range(len(pitches)):
           # print(pitches[i])
           # print(magnitudes[i])
           # print('\n')

	#get unique freq bins
        max_mag = np.amax(magnitudes, axis=1)
        freqs = list(set([min_freq + freq_bin_width * round((f[1] - min_freq) / freq_bin_width) for f in list(enumerate(np.amax(pitches, axis=1))) if f[1] >= min_freq and max_mag[f[0]] > 7]))
        return freqs
    return []
        
