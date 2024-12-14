package com.example.plantandsucculentapp.core.network

import android.util.Log
import io.grpc.*

private const val TAG = "GrpcInterceptor"

class GrpcLoggingInterceptor : ClientInterceptor {
    override fun <ReqT, RespT> interceptCall(
        method: MethodDescriptor<ReqT, RespT>,
        callOptions: CallOptions,
        next: Channel
    ): ClientCall<ReqT, RespT> {
        return object : ForwardingClientCall.SimpleForwardingClientCall<ReqT, RespT>(
            next.newCall(method, callOptions)
        ) {
            override fun start(responseListener: Listener<RespT>, headers: Metadata) {
                Log.d(TAG, "gRPC Call Started: ${method.fullMethodName}")
                Log.d(TAG, "Headers: $headers")

                super.start(object : ForwardingClientCallListener.SimpleForwardingClientCallListener<RespT>(responseListener) {
                    override fun onMessage(message: RespT) {
                        Log.d(TAG, "Response received: $message")
                        super.onMessage(message)
                    }

                    override fun onClose(status: Status, trailers: Metadata) {
                        Log.d(TAG, """
                            Call Completed:
                            Status: ${status.code}
                            Description: ${status.description}
                            Cause: ${status.cause?.message}
                            Trailers: $trailers
                        """.trimIndent())
                        super.onClose(status, trailers)
                    }

                    override fun onReady() {
                        Log.d(TAG, "Call Ready")
                        super.onReady()
                    }
                }, headers)
            }

            override fun sendMessage(message: ReqT) {
                Log.d(TAG, "Sending request: $message")
                super.sendMessage(message)
            }
        }
    }
} 