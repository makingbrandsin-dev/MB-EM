import os
import requests

def upload_apk():
    apk_path = "app/build/outputs/apk/debug/app-debug.apk"
    if not os.path.exists(apk_path):
        print("APK not found at", apk_path)
        return

    print("Fetching GoFile server...")
    try:
        servers_res = requests.get("https://api.gofile.io/servers", timeout=10)
        servers_data = servers_res.json()
        if servers_data.get("status") != "ok":
            print("Failed to get servers:", servers_data)
            return
        
        server = servers_data["data"]["servers"][0]["name"]
        upload_url = f"https://{server}.gofile.io/contents/uploadFile"
        print("Uploading to GoFile server:", server)

        with open(apk_path, "rb") as f:
            files = {"file": ("MB_EM.apk", f)}
            res = requests.post(upload_url, files=files, timeout=120)
            data = res.json()
            if data.get("status") == "ok":
                download_page = data["data"]["downloadPage"]
                print("SUCCESS_GOFILE_URL:", download_page)
            else:
                print("Upload failed:", data)
    except Exception as e:
        print("Error uploading to GoFile:", e)

if __name__ == "__main__":
    upload_apk()
