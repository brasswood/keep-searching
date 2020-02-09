#!/bin/python3
import subprocess
import json
import requests
import firebase_admin
from firebase_admin import credentials
from firebase_admin import db

scanCMD = "sudo iwlist wlan0 scan"
results = subprocess.check_output(scanCMD, shell=True)

splitResults = str(results).rsplit("Cell")[1:]

resultsArr = []

for result in splitResults:
    elements = str(result).rsplit("\\n")
    resultsArr.append(elements)

# Object to JSON
resultsObj = []

for result in resultsArr:
    obj = {}
    obj['macAddress'] = result[0][15:]
    obj['channel'] = result[1][28:]
    obj['age'] = 0
    obj['signalToNoiseRatio'] = 0

    indexForSlice = result[3].index("dBm")
    obj['signalStrength'] = result[3][(indexForSlice - 4):(indexForSlice - 1)]

    resultsObj.append(obj)

resultsJson = json.dumps(resultsObj)

parameters = {'key': '<Enter API Key Here>'}
payload = {'wifiAccessPoints': resultsObj}
r = requests.post('https://www.googleapis.com/geolocation/v1/geolocate', params=parameters, data=json.dumps(payload))
location = r.json()['location']

# Fetch the service account key JSON file contents
cred = credentials.Certificate('../auth/service-account-file.json')

# Initialize the app with a service account, granting admin privileges
firebase_admin.initialize_app(cred, {
    'databaseURL': 'https://<sub-domain>.firebaseio.com'
})

# Reference to database
piLocation = db.reference('piLocation')

# Open file that stores location
piLocation.set(location)
