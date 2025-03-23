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

from fastapi.responses import HTMLResponse
@app.get("/home", response_class=HTMLResponse)
async def home() :
    with open('main.html', 'r') as file:
        html = file.read()
    return html