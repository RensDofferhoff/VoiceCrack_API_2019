#!/usr/bin/python3
from flup.server.fcgi import WSGIServer
from app import app
import os

if __name__ == '__main__':
    WSGIServer(app).run()
    
