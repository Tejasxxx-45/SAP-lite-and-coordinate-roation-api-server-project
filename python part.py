from http.server import HTTPServer, BaseHTTPRequestHandler
import json, time, threading
PORT = 8000
COORDS = []
current = {"lat": 0.0, "lng": 0.0, "acc": 100.0}
def welcome():
    print("=" * 55)
    print('''
░█████╗░██╗░░░██╗░██████╗████████╗░█████╗░███╗░░░███╗  ██╗░░░░░░█████╗░░█████╗░░█████╗░████████╗██╗░█████╗░███╗░░██╗
██╔══██╗██║░░░██║██╔════╝╚══██╔══╝██╔══██╗████╗░████║  ██║░░░░░██╔══██╗██╔══██╗██╔══██╗╚══██╔══╝██║██╔══██╗████╗░██║
██║░░╚═╝██║░░░██║╚█████╗░░░░██║░░░██║░░██║██╔████╔██║  ██║░░░░░██║░░██║██║░░╚═╝███████║░░░██║░░░██║██║░░██║██╔██╗██║
██║░░██╗██║░░░██║░╚═══██╗░░░██║░░░██║░░██║██║╚██╔╝██║  ██║░░░░░██║░░██║██║░░██╗██╔══██║░░░██║░░░██║██║░░██║██║╚████║
╚█████╔╝╚██████╔╝██████╔╝░░░██║░░░╚█████╔╝██║░╚═╝░██║  ███████╗╚█████╔╝╚█████╔╝██║░░██║░░░██║░░░██║╚█████╔╝██║░╚███║
░╚════╝░░╚═════╝░╚═════╝░░░░╚═╝░░░░╚════╝░╚═╝░░░░░╚═╝  ╚══════╝░╚════╝░░╚════╝░╚═╝░░╚═╝░░░╚═╝░░░╚═╝░╚════╝░╚═╝░░╚══╝

██████╗░░█████╗░████████╗░█████╗░████████╗░█████╗░██████╗░  ░██████╗███████╗██████╗░██╗░░░██╗███████╗██████╗░
██╔══██╗██╔══██╗╚══██╔══╝██╔══██╗╚══██╔══╝██╔══██╗██╔══██╗  ██╔════╝██╔════╝██╔══██╗██║░░░██║██╔════╝██╔══██╗
██████╔╝██║░░██║░░░██║░░░███████║░░░██║░░░██║░░██║██████╔╝  ╚█████╗░█████╗░░██████╔╝╚██╗░██╔╝█████╗░░██████╔╝
██╔══██╗██║░░██║░░░██║░░░██╔══██║░░░██║░░░██║░░██║██╔══██╗  ░╚═══██╗██╔══╝░░██╔══██╗░╚████╔╝░██╔══╝░░██╔══██╗
██║░░██║╚█████╔╝░░░██║░░░██║░░██║░░░██║░░░╚█████╔╝██║░░██║  ██████╔╝███████╗██║░░██║░░╚██╔╝░░███████╗██║░░██║
╚═╝░░╚═╝░╚════╝░░░░╚═╝░░░╚═╝░░╚═╝░░░╚═╝░░░░╚════╝░╚═╝░░╚═╝  ╚═════╝░╚══════╝╚═╝░░╚═╝░░░╚═╝░░░╚══════╝╚═╝░░╚═╝
          ''')
    print("=" * 55)
    print("This server will:")
    print("• Take 6 custom coordinates (lat, lng, accuracy)")
    print("• Rotate between them every 10 minutes")
    print("• Serve them via http://127.0.0.1:8000/")
    print("=" * 55)
    print()
def load_coords():
    global COORDS
    print("Enter 6 coordinates (latitude, longitude, accuracy):\n")

    for i in range(1, 7):
        print(f"--- Coordinate {i} ---")
        lat = float(input("Latitude : "))
        lng = float(input("Longitude: "))
        acc = float(input("Accuracy : "))
        COORDS.append((lat, lng, acc))
        print()
    current['lat'], current['lng'], current['acc'] = COORDS[0]
def start_rotation(interval=600): 
    index = 0
    while True:
        lat, lng, acc = COORDS[index]
        current['lat'] = lat
        current['lng'] = lng
        current['acc'] = acc

        print(time.strftime("%Y-%m-%d %H:%M:%S"), 
              f"→ Rotated to index {index+1}: ({lat}, {lng}) acc={acc}")

        index = (index + 1) % len(COORDS)
        time.sleep(interval)
class Handler(BaseHTTPRequestHandler):
    def do_GET(self):
        resp = {
            "location": {
                "lat": current['lat'],
                "lng": current['lng']
            },
            "accuracy": current['acc']
        }
        b = json.dumps(resp).encode('utf-8')

        self.send_response(200)
        self.send_header('Content-Type', 'application/json')
        self.send_header('Content-Length', str(len(b)))
        self.end_headers()
        self.wfile.write(b)
def main():
    welcome()
    load_coords()
    print("✔ Starting 10‑minute rotation thread…")
    t = threading.Thread(target=start_rotation, daemon=True)
    t.start()
    print(f"✔ Serving API on http://127.0.0.1:{PORT}/")
    HTTPServer(('127.0.0.1', PORT), Handler).serve_forever()
main()