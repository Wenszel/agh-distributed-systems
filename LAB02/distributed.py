from fastapi import FastAPI
import requests
from key import ALPHAVANTAGE_KEY, CURRENCYAPI_KEY 
import json

app=FastAPI( )

@app.get("/")
async def root() :
    with open('iwda.json', 'r') as file:
        data = json.load(file)
    # x = requests.get(uri)
    # print(x)
    return {"message" : data}
