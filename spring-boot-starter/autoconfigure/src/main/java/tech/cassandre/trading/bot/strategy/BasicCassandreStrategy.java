package tech.cassandre.trading.bot.strategy;

import tech.cassandre.trading.bot.dto.market.TickerDTO;
import tech.cassandre.trading.bot.dto.position.PositionDTO;
import tech.cassandre.trading.bot.dto.position.PositionStatusDTO;
import tech.cassandre.trading.bot.dto.trade.OrderDTO;
import tech.cassandre.trading.bot.dto.trade.TradeDTO;
import tech.cassandre.trading.bot.dto.user.AccountDTO;

/**
 * Basic strategy - Cassandre bot will run the first BasicCassandreStrategy implementation found.
 */
@SuppressWarnings("unused")
public abstract class BasicCassandreStrategy extends GenericCassandreStrategy {

    @Override
    public final void accountUpdate(final AccountDTO account) {
        getAccounts().put(account.getId(), account);
        onAccountUpdate(account);
    }

    @Override
    public final void tickerUpdate(final TickerDTO ticker) {
        getLastTicker().put(ticker.getCurrencyPair(), ticker);
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
