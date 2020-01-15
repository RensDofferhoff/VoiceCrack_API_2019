# Voice Crack Client
### Rens Dofferhoff and Rick Boks

Voice Crack is a secure login mechanism using voice authentication and an additional security layer. While logging in, a high-pitched noise is played in the background which protects you against replay attacks. 

This app acts as an interface to Voice Crack and uses the [Microsoft Speaker Recognition API](https://docs.microsoft.com/en-us/azure/cognitive-services/speaker-recognition/home). You can't use it to log into anything, but it is merely a proof of concept. The app provides the following functionality:

1. Register
	* Choose a username
	* Record your voice while saying one of the supported phrases (see [Microsoft's Documentation](https://azure.microsoft.com/en-us/services/cognitive-services/speaker-recognition/)) for at least 3 times and press "Submit"
	* You are now ready to log in!

2. Logging in
	* Enter your username
	* Make sure your sound is enabled
	* Record your voice while saying your authentication phrase while the noise is played in the background
	* You are securely logged in!

An APK that can be installed on any Android device with Android 8.0 or higher can be found in the folder `/APK`.
