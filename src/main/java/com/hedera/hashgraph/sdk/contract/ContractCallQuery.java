package com.hedera.hashgraph.sdk.contract;

import com.google.protobuf.ByteString;
import com.hedera.hashgraph.proto.ContractCallLocalQuery;
import com.hedera.hashgraph.proto.Query;
import com.hedera.hashgraph.proto.QueryHeader;
import com.hedera.hashgraph.proto.Response;
import com.hedera.hashgraph.proto.SmartContractServiceGrpc;
import com.hedera.hashgraph.sdk.Client;
import com.hedera.hashgraph.sdk.HederaNetworkException;
import com.hedera.hashgraph.sdk.HederaStatusException;
import com.hedera.hashgraph.sdk.HederaThrowable;
import com.hedera.hashgraph.sdk.QueryBuilder;

import java.util.function.Consumer;

import io.grpc.MethodDescriptor;

/**
 * Call a function without updating its state or requiring concensus.
 */
// `ContractCallLocalQuery`
public class ContractCallQuery extends QueryBuilder<ContractFunctionResult, ContractCallQuery> {
    private final ContractCallLocalQuery.Builder builder = inner.getContractCallLocalBuilder();

    public ContractCallQuery() {
        super();
    }

    @Override
    protected QueryHeader.Builder getHeaderBuilder() {
        return builder.getHeaderBuilder();
    }

    @Override
    protected MethodDescriptor<Query, Response> getMethod() {
        return SmartContractServiceGrpc.getContractCallLocalMethodMethod();
    }

    public ContractCallQuery setContractId(ContractId id) {
        builder.setContractID(id.toProto());
        return this;
    }

    public ContractCallQuery setGas(long gas) {
        builder.setGas(gas);
        return this;
    }

    @Override
    public long getCost(Client client) throws HederaStatusException, HederaNetworkException {
        // network bug: ContractCallLocal cost estimate is too low
        return (long) (super.getCost(client) * 1.1);
    }

    @Override
    public void getCostAsync(Client client, Consumer<Long> withCost, Consumer<HederaThrowable> onError) {
        // network bug: ContractCallLocal cost estimate is too low
        super.getCostAsync(client, cost -> withCost.accept((long) (cost * 1.1)), onError);
    }

    /**
     * Set the function parameters (selector plus arguments) as a raw byte array.
     *
     * @param parameters
     * @return {@code this} for fluent API usage.
     */
    public ContractCallQuery setFunctionParams(byte[] parameters) {
        builder.setFunctionParameters(ByteString.copyFrom(parameters));
        return this;
    }

    /**
     * Set the function to call with an <b>empty parameter list</b>.
     *
     * @param funcName the name of the function to call.
     * @return {@code this} for fluent API usage.
     */
    public ContractCallQuery setFunction(String funcName) {
        return setFunction(funcName, new ContractFunctionParams());
    }

    /**
     * Set the function to call and the parameters to pass.
     *
     * @param funcName the name of the function to call; the function selector is calculated from
     *                 this and the types of the params passed in {@code params}.
     * @param params the params to pass to the function being executed.
     * @return {@code this} for fluent API usage.
     */
    public ContractCallQuery setFunction(String funcName, ContractFunctionParams params) {
        builder.setFunctionParameters(params.toBytes(funcName));
        return this;
    }

    public ContractCallQuery setMaxResultSize(long size) {
        builder.setMaxResultSize(size);
        return this;
    }

    @Override
    protected void doValidate() {
        require(builder.hasContractID(), ".setContractId() required");
    }

    @Override
    protected ContractFunctionResult extractResponse(Response raw) {
        if (raw.hasContractCallLocal()) {
            return new ContractFunctionResult(raw.getContractCallLocal().getFunctionResultOrBuilder());
        } else {
            throw new IllegalStateException("response case not ContractCallLocal: " + raw.getResponseCase());
        }
    }
}
