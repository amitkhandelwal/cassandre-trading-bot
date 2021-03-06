package tech.cassandre.trading.bot.strategy;

import com.google.common.base.MoreObjects;
import org.ta4j.core.BarSeries;
import org.ta4j.core.BaseBarSeriesBuilder;
import org.ta4j.core.Strategy;
import org.ta4j.core.num.DoubleNum;
import tech.cassandre.trading.bot.dto.market.TickerDTO;
import tech.cassandre.trading.bot.dto.position.PositionDTO;
import tech.cassandre.trading.bot.dto.position.PositionStatusDTO;
import tech.cassandre.trading.bot.dto.trade.OrderDTO;
import tech.cassandre.trading.bot.dto.trade.TradeDTO;
import tech.cassandre.trading.bot.dto.user.AccountDTO;
import tech.cassandre.trading.bot.dto.util.CurrencyPairDTO;

import java.math.BigDecimal;
import java.time.Duration;
import java.time.ZonedDateTime;
import java.util.LinkedHashSet;
import java.util.Optional;
import java.util.Set;

/**
 * Basic ta4j strategy.
 */
@SuppressWarnings("unused")
public abstract class BasicTa4jCassandreStrategy extends GenericCassandreStrategy {

    /** Timestamp of the last added bar. */
    private ZonedDateTime lastAddedBarTimestamp;

    /** Series. */
    private final BarSeries series;

    /** Strategy. */
    private final Strategy strategy;

    /**
     * Constructor.
     */
    public BasicTa4jCassandreStrategy() {
        // Build the series.
        series = new BaseBarSeriesBuilder()
                .withNumTypeOf(DoubleNum.class)
                .withName(getRequestedCurrencyPair().toString())
                .build();
        series.setMaximumBarCount(getMaximumBarCount());

        // Build the strategy.public abstract
        strategy = getStrategy();
    }

    /**
     * Implements this method to tell the bot which currency pair your strategy will receive.
     *
     * @return the list of currency pairs tickers your want to receive
     */
    public abstract CurrencyPairDTO getRequestedCurrencyPair();

    /**
     * Implements this method to tell the bot how many bars you want to keep in your bar series.
     *
     * @return maximum bar count.
     */
    @SuppressWarnings("SameReturnValue")
    public abstract int getMaximumBarCount();

    /**
     * Implements this method to set the time between two bars are added.
     *
     * @return temporal amount
     */
    public abstract Duration getDelayBetweenTwoBars();

    /**
     * Implements this method to tell the bot which strategy to apply.
     *
     * @return strategy
     */
    public abstract Strategy getStrategy();

    @Override
    public final Set<CurrencyPairDTO> getRequestedCurrencyPairs() {
        // We only support one currency pair with this strategy.
        return Set.of(getRequestedCurrencyPair());
    }

    @Override
    public final void accountUpdate(final AccountDTO account) {
        getAccounts().put(account.getId(), account);
        onAccountUpdate(account);
    }

    @Override
    public final void tickerUpdate(final TickerDTO ticker) {
        getLastTicker().put(ticker.getCurrencyPair(), ticker);
        // If there is no bar or if the duration between the last bar and the ticker is enough.
        if (lastAddedBarTimestamp == null
                || ticker.getTimestamp().isEqual(lastAddedBarTimestamp.plus(getDelayBetweenTwoBars()))
                || ticker.getTimestamp().isAfter(lastAddedBarTimestamp.plus(getDelayBetweenTwoBars()))) {

            // Add the ticker to the series.
            Number openPrice = MoreObjects.firstNonNull(ticker.getOpen(), 0);
            Number highPrice = MoreObjects.firstNonNull(ticker.getHigh(), 0);
            Number lowPrice = MoreObjects.firstNonNull(ticker.getLow(), 0);
            Number closePrice = MoreObjects.firstNonNull(ticker.getLast(), 0);
            Number volume = MoreObjects.firstNonNull(ticker.getVolume(), 0);
            series.addBar(ticker.getTimestamp(), openPrice, highPrice, lowPrice, closePrice, volume);
            lastAddedBarTimestamp = ticker.getTimestamp();

            // Ask what to do to the strategy.
            int endIndex = series.getEndIndex();
            if (strategy.shouldEnter(endIndex)) {
                // Our strategy should enter.
                shouldEnter();
            } else if (strategy.shouldExit(endIndex)) {
                // Our strategy should exit.
                shouldExit();
            }
        }
        onTickerUpdate(ticker);
    }

    @Override
    public final void orderUpdate(final OrderDTO order) {
        getOrders().put(order.getId(), order);
        onOrderUpdate(order);
    }

    @Override
    public final void tradeUpdate(final TradeDTO trade) {
        getTrades().put(trade.getId(), trade);
        onTradeUpdate(trade);
    }

    @Override
    public final void positionUpdate(final PositionDTO position) {
        // For every position update.
        getPositions().put(position.getId(), position);
        onPositionUpdate(position);

        // For every position status update.
        PositionStatusDTO previousPosition = getPreviousPositions().get(position.getId());
        if (previousPosition == null || !previousPosition.equals(position.getStatus())) {
            getPreviousPositions().put(position.getId(), position.getStatus());
            onPositionStatusUpdate(position);
        }
    }

    /**
     * Returns true if we have enough assets to buy.
     *
     * @param amount amount
     * @return true if we there is enough money to buy
     */
    public final boolean canBuy(final BigDecimal amount) {
        final Optional<AccountDTO> tradeAccount = getTradeAccount(new LinkedHashSet<>(getAccounts().values()));
        return tradeAccount.filter(accountDTO -> canBuy(accountDTO, getRequestedCurrencyPair(), amount)).isPresent();
    }

    /**
     * Returns true if we have enough assets to buy.
     *
     * @param amount              amount
     * @param minimumBalanceAfter minimum balance that should be left after buying
     * @return true if we there is enough money to buy
     */
    public final boolean canBuy(final BigDecimal amount,
                                final BigDecimal minimumBalanceAfter) {
        final Optional<AccountDTO> tradeAccount = getTradeAccount(new LinkedHashSet<>(getAccounts().values()));
        return tradeAccount.filter(accountDTO -> canBuy(accountDTO, getRequestedCurrencyPair(), amount, minimumBalanceAfter)).isPresent();
    }

    /**
     * Returns true if we have enough assets to buy.
     *
     * @param account account
     * @param amount  amount
     * @return true if we there is enough money to buy
     */
    public final boolean canBuy(final AccountDTO account,
                                final BigDecimal amount) {
        return canBuy(account, getRequestedCurrencyPair(), amount);
    }

    /**
     * Returns true if we have enough assets to buy and if minimumBalanceAfter is left on the account after.
     *
     * @param account             account
     * @param amount              amount
     * @param minimumBalanceAfter minimum balance that should be left after buying
     * @return true if we there is enough money to buy
     */
    public final boolean canBuy(final AccountDTO account,
                                final BigDecimal amount,
                                final BigDecimal minimumBalanceAfter) {
        return canBuy(account, getRequestedCurrencyPair(), amount, minimumBalanceAfter);
    }

    /**
     * Returns true if we have enough assets to sell.
     *
     * @param amount              amount
     * @param minimumBalanceAfter minimum balance that should be left after buying
     * @return true if we there is enough money to buy
     */
    public final boolean canSell(final BigDecimal amount,
                                 final BigDecimal minimumBalanceAfter) {
        final Optional<AccountDTO> tradeAccount = getTradeAccount(new LinkedHashSet<>(getAccounts().values()));
        return tradeAccount.filter(accountDTO -> canSell(accountDTO, getRequestedCurrencyPair().getBaseCurrency(), amount, minimumBalanceAfter)).isPresent();
    }

    /**
     * Returns true if we have enough assets to sell.
     *
     * @param amount amount
     * @return true if we there is enough money to buy
     */
    public final boolean canSell(final BigDecimal amount) {
        final Optional<AccountDTO> tradeAccount = getTradeAccount(new LinkedHashSet<>(getAccounts().values()));
        return tradeAccount.filter(accountDTO -> canSell(accountDTO, getRequestedCurrencyPair().getBaseCurrency(), amount)).isPresent();
    }

    /**
     * Returns true if we have enough assets to sell.
     *
     * @param account account
     * @param amount  amount
     * @return true if we there is enough money to buy
     */
    public final boolean canSell(final AccountDTO account,
                                 final BigDecimal amount) {
        return canSell(account, getRequestedCurrencyPair().getBaseCurrency(), amount);
    }

    /**
     * Returns true if we have enough assets to sell and if minimumBalanceAfter is left on the account after.
     *
     * @param account             account
     * @param amount              amount
     * @param minimumBalanceAfter minimum balance that should be left after selling
     * @return true if we there is enough money to buy
     */
    public final boolean canSell(final AccountDTO account,
                                 final BigDecimal amount,
                                 final BigDecimal minimumBalanceAfter) {
        return canSell(account, getRequestedCurrencyPair().getBaseCurrency(), amount, minimumBalanceAfter);
    }

    /**
     * Called when your strategy says you should enter.
     */
    public abstract void shouldEnter();

    /**
     * Called when your strategy says your should exit.
     */
    public abstract void shouldExit();

    /**
     * Getter for series.
     *
     * @return series
     */
    public final BarSeries getSeries() {
        return series;
    }

    @Override
    public void onAccountUpdate(final AccountDTO account) {

    }

    @Override
    public void onTickerUpdate(final TickerDTO ticker) {

    }

    @Override
    public void onOrderUpdate(final OrderDTO order) {

    }

    @Override
    public void onTradeUpdate(final TradeDTO trade) {

    }

    @Override
    public void onPositionUpdate(final PositionDTO position) {

    }

    @Override
    public void onPositionStatusUpdate(final PositionDTO position) {

    }

}
