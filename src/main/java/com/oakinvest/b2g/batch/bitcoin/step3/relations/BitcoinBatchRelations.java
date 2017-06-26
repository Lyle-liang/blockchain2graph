package com.oakinvest.b2g.batch.bitcoin.step3.relations;

import com.oakinvest.b2g.domain.bitcoin.BitcoinAddress;
import com.oakinvest.b2g.domain.bitcoin.BitcoinBlock;
import com.oakinvest.b2g.domain.bitcoin.BitcoinBlockState;
import com.oakinvest.b2g.domain.bitcoin.BitcoinTransaction;
import com.oakinvest.b2g.domain.bitcoin.BitcoinTransactionOutput;
import com.oakinvest.b2g.repository.bitcoin.BitcoinRepositories;
import com.oakinvest.b2g.service.BitcoinDataService;
import com.oakinvest.b2g.service.StatusService;
import com.oakinvest.b2g.util.bitcoin.batch.BitcoinBatchTemplate;
import org.springframework.stereotype.Component;

import java.util.Objects;
import java.util.Optional;

/**
 * Bitcoin import relations batch.
 * Created by straumat on 27/02/17.
 */
@Component
public class BitcoinBatchRelations extends BitcoinBatchTemplate {

    /**
     * Log prefix.
     */
    private static final String PREFIX = "Relations batch";

    /**
     * Constructor.
     *
     * @param newBitcoinRepositories    bitcoin repositories
     * @param newBitcoinDataService     bitcoin data service
     * @param newStatus                 status
     */
    public BitcoinBatchRelations(final BitcoinRepositories newBitcoinRepositories, final BitcoinDataService newBitcoinDataService, final StatusService newStatus) {
        super(newBitcoinRepositories, newBitcoinDataService, newStatus);
    }

    /**
     * Returns the log prefix to display in each log.
     */
    @Override
    public final String getLogPrefix() {
        return PREFIX;
    }

    /**
     * Return the block to process.
     *
     * @return block to process.
     */
    @Override
    protected final Optional<Long> getBlockHeightToProcess() {
        BitcoinBlock blockToTreat = getBlockRepository().findFirstBlockByState(BitcoinBlockState.ADDRESSES_IMPORTED);
        if (blockToTreat != null) {
            return Optional.of(blockToTreat.getHeight());
        } else {
            return Optional.empty();
        }
    }

    /**
     * Process block.
     *
     * @param blockHeight block height to process.
     */
    @Override
    protected final Optional<BitcoinBlock> processBlock(final long blockHeight) {
        final BitcoinBlock blockToTreat = getBlockRepository().findByHeight(blockHeight);

        // -----------------------------------------------------------------------------------------------------
        // we link the addresses to the input and the origin transaction.
        blockToTreat.getTx()
                .forEach(
                        txId -> {
                            addLog("- Transaction " + txId);
                            BitcoinTransaction t = getTransactionRepository().findByTxId(txId);

                            // For each Vin.
                            t.getInputs()
                                    .stream()
                                    // If it's NOT a coinbase transaction.
                                    .filter(vin -> !vin.isCoinbase())
                                    .forEach(vin -> {
                                        // We retrieve the original transaction.
                                        BitcoinTransaction originTransaction = getTransactionRepository().findByTxId(vin.getTxId());
                                        if (originTransaction != null) {

                                            // We retrieve the original transaction output.
                                            Optional<BitcoinTransactionOutput> originTransactionOutput = originTransaction.getOutputByIndex(vin.getvOut());
                                            if (originTransactionOutput.isPresent()) {
                                                // We set the addresses "from" if it's not a coinbase transaction.
                                                vin.setTransactionOutput(originTransactionOutput.get());

                                                // We set all the addresses linked to this input
                                                originTransactionOutput.get().getAddresses()
                                                        .stream()
                                                        .filter(Objects::nonNull)
                                                        .forEach(a -> {
                                                            BitcoinAddress address = getAddressRepository().findByAddress(a);
                                                            address.getInputTransactions().add(vin);
                                                            getAddressRepository().save(address);
                                                        });
                                                addLog("-- Done processing vin : " + vin);
                                            } else {
                                                addError("Impossible to find the original output transaction " + vin.getTxId() + " / " + vin.getvOut());
                                                throw new RuntimeException("Impossible to find the original output transaction " + vin.getTxId() + " / " + vin.getvOut());
                                            }
                                        } else {
                                            addError("Impossible to find the original transaction " + vin.getTxId());
                                            throw new RuntimeException("Impossible to find the original transaction " + vin.getTxId());
                                        }
                                    });

                            // For each Vout.
                            t.getOutputs()
                                    .forEach(vout -> {
                                        vout.getAddresses().stream()
                                                .filter(Objects::nonNull)
                                                .forEach(a -> {
                                                    BitcoinAddress address = getAddressRepository().findByAddress(a);
                                                    address.getOutputTransactions().add(vout);
                                                    getAddressRepository().save(address);
                                                });
                                        addLog("-- Done processing vout : " + vout);
                                    });

                        }
                );
        return Optional.of(blockToTreat);
    }

    /**
     * Return the state to set to the block that has been processed.
     *
     * @return state to set of the block that has been processed.
     */
    @Override
    protected final BitcoinBlockState getNewStateOfProcessedBlock() {
        return BitcoinBlockState.IMPORTED;
    }

}