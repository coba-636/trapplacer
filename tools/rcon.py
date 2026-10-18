import socket, struct, sys, time

class Rcon:
    def __init__(self, host='127.0.0.1', port=25575, password='test'):
        self.s = socket.create_connection((host, port), timeout=30)
        self.rid = 0
        self._send(3, password)
        self._recv()

    def _send(self, typ, body):
        self.rid += 1
        data = struct.pack('<ii', self.rid, typ) + body.encode('utf-8') + b'\x00\x00'
        self.s.sendall(struct.pack('<i', len(data)) + data)

    def _recvn(self, n):
        buf = b''
        while len(buf) < n:
            chunk = self.s.recv(n - len(buf))
            if not chunk:
                raise IOError('closed')
            buf += chunk
        return buf

    def _recv(self):
        (length,) = struct.unpack('<i', self._recvn(4))
        data = self._recvn(length)
        rid, typ = struct.unpack('<ii', data[:8])
        return data[8:-2].decode('utf-8', 'replace')

    def cmd(self, command):
        self._send(2, command)
        return self._recv()

if __name__ == '__main__':
    r = Rcon()
    for c in sys.argv[1:]:
        print('>', c)
        print(r.cmd(c))
