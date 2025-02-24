package com.gzh.remoting.transport;

import com.gzh.extension.SPI;
import com.gzh.remoting.dto.RpcRequest;

@SPI
public interface RpcRequestTransport {
    Object sendRpsRequest(RpcRequest rpcRequest);
}
