#!/bin/zsh
#sh gen_proto.sh ../go-genproto proto/autohome/api/annotations.proto
#$1 生成的目录
#$2 需要编译的proto

PROTOC_GEN_GO="$GOPATH/bin/protoc-gen-go"
PROTOC="protoc"

gen_proto() {
  $PROTOC  --proto_path=proto  --go_out=paths=source_relative:"$1" --go-triple_out=paths=source_relative:"$1"  "$2"
}

program_exists() {
    local ret='0'
    command -v "$1" >/dev/null 2>&1 || { local ret='1'; }

    # fail on non-zero return value
    if [ "$ret" -ne 0 ]; then
        return 1
    fi
    return 0
}

program_exists "$PROTOC"
if [ $? -eq 1 ];then
  echo 'install protoc...'
  if [ "$(uname)" == "Darwin" ]; then
    brew install protobuf
  elif [ "$(uname)" == "Linux" ]; then
    sudo apt-get install protobuf-compiler
  fi
fi

program_exists "$PROTOC_GEN_GO"
if [ $? -eq 1 ];then
  echo 'install protoc_gen_go...'
  go install google.golang.org/protobuf/cmd/protoc-gen-go@latest
  go install github.com/dubbogo/tools/cmd/protoc-gen-dubbo3grpc@latest
  go install google.golang.org/grpc/cmd/protoc-gen-go-grpc@latest
fi

gen_proto "$1" "$2"