.PHONY: download test push-docker clean

PROTOC_VERSION := 0.4.0

download: download-proto-compiler

download-proto-compiler:
	docker pull znly/protoc:${PROTOC_VERSION}

check-env:
	docker version && go env

generate: generate-git generate-protobufs

GIT_COMMIT := $(shell git rev-list -1 HEAD)
STATIC_JSON := ./handlers/static/git.json
PROTO_DIR := ./proto

generate-git:
	echo "{\"commit\": \"$(GIT_COMMIT)\", \"microservice\": \"chat\"}" > ${STATIC_JSON}

PROTOBUF_SRC_DIR = $(shell cd ..; pwd)

generate-protobufs:
	mkdir -p ${PROTO_DIR} && \
	docker run -t --rm -v $(PROTOBUF_SRC_DIR):/ws -w /ws znly/protoc:${PROTOC_VERSION} --go_out=plugins=grpc:chat/proto -I./protobuf ./protobuf/$(notdir $(wildcard ../protobuf/*.proto))

test:
	go test ./... -count=1 -test.v -test.timeout=20s -p 1

package-go:
	CGO_ENABLED=0 go build -trimpath -ldflags '-w -extldflags "-static"'

BUILDDIR := ./build
EXECUTABLE := chat
IMAGE = nkonev/chat:latest

package-docker:
	mkdir -p $(BUILDDIR) && \
	cp $(EXECUTABLE) $(BUILDDIR) && cp ./Dockerfile $(BUILDDIR) && \
	echo "Will build docker chat image" && \
 	docker build --build-arg BINARY=$(EXECUTABLE) -t $(IMAGE) $(BUILDDIR)

package: package-go package-docker

push-docker:
	echo "Will push docker chat image" && \
	docker push $(IMAGE)

clean:
	rm -rf ${STATIC_JSON} "${PROTO_DIR}" ${EXECUTABLE} ${BUILDDIR}