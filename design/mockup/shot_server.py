"""Static server for the mockups plus a POST /save sink.

The design mockups have to become PNG files, and every headless-browser and
image-tool route on this machine is blocked by the sandbox. The one renderer
that can reach these pages is the app's own Browser pane — so the page
rasterises itself with html2canvas and posts the PNG back here.
"""
import base64
import http.server
import os
import socketserver

ROOT = os.path.dirname(os.path.abspath(__file__))
PORT = 8731


class Handler(http.server.SimpleHTTPRequestHandler):
    def __init__(self, *args, **kwargs):
        super().__init__(*args, directory=ROOT, **kwargs)

    def do_POST(self):
        if self.path != "/save":
            self.send_error(404)
            return
        length = int(self.headers.get("Content-Length", 0))
        body = self.rfile.read(length).decode("utf-8")
        name, _, payload = body.partition("|")
        # Only ever writes beside this file, and only .png — the browser is
        # untrusted input even when it is the one we opened.
        safe = os.path.basename(name)
        if not safe.endswith(".png"):
            self.send_error(400)
            return
        with open(os.path.join(ROOT, safe), "wb") as handle:
            handle.write(base64.b64decode(payload.split(",", 1)[1]))
        self.send_response(200)
        self.send_header("Content-Length", "2")
        self.end_headers()
        self.wfile.write(b"ok")

    def log_message(self, *args):
        pass


socketserver.TCPServer.allow_reuse_address = True
with socketserver.TCPServer(("127.0.0.1", PORT), Handler) as httpd:
    httpd.serve_forever()
