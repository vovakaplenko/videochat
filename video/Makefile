.PHONY: test push-docker clean

download:
	echo "Nothing to download"

check-env:
	docker version && go env

generate: generate-git

GIT_COMMIT := $(shell git rev-list -1 HEAD)
STATIC_JSON := ./handlers/static/git.json

generate-git:
	echo "{\"commit\": \"$(GIT_COMMIT)\", \"microservice\": \"video\"}" > ${STATIC_JSON}

test:
	go test ./... -count=1 -test.v -test.timeout=20s -p 1

package-go:
	CGO_ENABLED=0 go build -trimpath -ldflags '-w -extldflags "-static"'

BUILDDIR := ./build
EXECUTABLE := video
IMAGE = nkonev/chat-video:latest

package-docker:
	mkdir -p $(BUILDDIR) && \
	cp $(EXECUTABLE) $(BUILDDIR) && cp ./Dockerfile $(BUILDDIR) && \
	echo "Will build docker video image" && \
 	docker build --build-arg BINARY=$(EXECUTABLE) -t $(IMAGE) $(BUILDDIR)

package: package-go package-docker

push-docker:
	echo "Will push docker video image" && \
	docker push $(IMAGE)

clean:
	rm -rf ${STATIC_JSON} ${EXECUTABLE} ${BUILDDIR}