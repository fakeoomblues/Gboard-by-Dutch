"""Serve authored validation media and a minimal Stremio add-on on loopback only."""
from http.server import BaseHTTPRequestHandler,ThreadingHTTPServer
from pathlib import Path
from urllib.parse import urlsplit
import argparse,json

class Fixture(BaseHTTPRequestHandler):
    def do_GET(self):
        path=urlsplit(self.path).path
        if path=="/addon/manifest.json":
            return self.send_json({"id":"nl.johninnl.wuplay.car.validation","version":"1.0.0","name":"WuPlay Car Validation","description":"Authored emulator validation clip","resources":["stream","subtitles"],"types":["movie","series"],"catalogs":[]})
        if path.startswith("/addon/stream/"):
            return self.send_json({"streams":[{"name":"Car validation","title":"Authored test video with request-header check","url":self.server.base+"/car-test.mp4","behaviorHints":{"proxyHeaders":{"request":{"X-WuPlay-Car":"validation"}}}}]})
        if path.startswith("/addon/subtitles/"):
            return self.send_json({"subtitles":[{"id":"car-validation-en","lang":"eng","name":"Car validation captions","url":self.server.base+"/subtitle.srt"}]})
        if path=="/subtitle.srt":
            body=b"1\n00:00:00,000 --> 00:01:00,000\nWuPlay car subtitle validation\n"
            return self.send_bytes(body,"application/x-subrip")
        if path in ("/plain-test.mp4","/car-test.mp4"):
            if path=="/car-test.mp4" and self.headers.get("X-WuPlay-Car")!="validation":
                self.send_error(403,"Validation header required");return
            return self.send_media()
        self.send_error(404)
    def send_media(self):
        total=self.server.media.stat().st_size;start=0;end=total-1;status=200
        requested=self.headers.get("Range")
        if requested:
            import re
            match=re.fullmatch(r"bytes=(\d+)-(\d*)",requested)
            if not match:self.send_error(416);return
            start=int(match[1]);end=min(int(match[2]) if match[2] else end,end);status=206
            if start>end:self.send_error(416);return
        self.send_response(status);self.send_header("Content-Type","video/mp4");self.send_header("Accept-Ranges","bytes")
        self.send_header("Content-Length",str(end-start+1))
        if status==206:self.send_header("Content-Range",f"bytes {start}-{end}/{total}")
        self.end_headers()
        try:
            with self.server.media.open("rb") as source:
                source.seek(start);remaining=end-start+1
                while remaining:
                    data=source.read(min(65536,remaining))
                    if not data:break
                    self.wfile.write(data);remaining-=len(data)
        except (BrokenPipeError,ConnectionResetError):pass
    def send_json(self,value):self.send_bytes(json.dumps(value).encode(),"application/json")
    def send_bytes(self,body,content_type):
        self.send_response(200);self.send_header("Content-Type",content_type);self.send_header("Content-Length",str(len(body)));self.end_headers()
        try:self.wfile.write(body)
        except (BrokenPipeError,ConnectionResetError):pass
    def log_message(self,fmt,*args):
        print("fixture",urlsplit(self.path).path,"status",args[1] if len(args)>1 else "-",flush=True)

if __name__=="__main__":
    parser=argparse.ArgumentParser(description=__doc__);parser.add_argument("media",type=Path);parser.add_argument("--port",type=int,default=8769)
    args=parser.parse_args();server=ThreadingHTTPServer(("127.0.0.1",args.port),Fixture);server.media=args.media;server.base=f"http://10.0.2.2:{args.port}";server.serve_forever()
