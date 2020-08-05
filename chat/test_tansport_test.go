package main

import (
	"github.com/centrifugal/centrifuge"
	"io"
	"sync"
)

func stringInSlice(a string, list []string) bool {
	for _, b := range list {
		if b == a {
			return true
		}
	}
	return false
}

type testTransport struct {
	mu         sync.Mutex
	sink       chan []byte
	closed     bool
	closeCh    chan struct{}
	disconnect *centrifuge.Disconnect
	protoType  centrifuge.ProtocolType
}

func newTestTransport() *testTransport {
	return &testTransport{
		protoType: centrifuge.ProtocolTypeJSON,
		closeCh:   make(chan struct{}),
	}
}

func (t *testTransport) setProtocolType(pType centrifuge.ProtocolType) {
	t.protoType = pType
}

func (t *testTransport) setSink(sink chan []byte) {
	t.sink = sink
}

func (t *testTransport) Write(data []byte) error {
	t.mu.Lock()
	defer t.mu.Unlock()
	if t.closed {
		return io.EOF
	}
	dataCopy := make([]byte, len(data))
	copy(dataCopy, data)
	if t.sink != nil {
		t.sink <- dataCopy
	}
	return nil
}

func (t *testTransport) Name() string {
	return "test_transport"
}

func (t *testTransport) Protocol() centrifuge.ProtocolType {
	return t.protoType
}

func (t *testTransport) Encoding() centrifuge.EncodingType {
	return centrifuge.EncodingTypeJSON
}

func (t *testTransport) Close(disconnect *centrifuge.Disconnect) error {
	t.mu.Lock()
	defer t.mu.Unlock()
	if t.closed {
		return nil
	}
	t.disconnect = disconnect
	t.closed = true
	close(t.closeCh)
	return nil
}
