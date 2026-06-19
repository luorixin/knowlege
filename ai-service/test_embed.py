import httpx

url = "https://dashscope.aliyuncs.com/compatible-mode/v1/embeddings"
headers = {
    "Content-Type": "application/json",
    "Authorization": "Bearer sk-880c42d31eba49019b67378defa665a3"
}
payload = {
    "model": "text-embedding-v4",
    "input": ["Hello"],
    "dimensions": 128
}

print("Requesting...")
try:
    with httpx.Client(timeout=60, trust_env=False) as client:
        response = client.post(url, headers=headers, json=payload)
        print("Status code:", response.status_code)
        if response.status_code != 200:
            print("Response:", response.text)
        else:
            print("Success")
except Exception as e:
    print("Error:", e)
