public enum Currency {

    RUB("RUB", "₽", 100),
    USD("USD", "$", 9_600),
    EUR("EUR", "€", 10_300),
    CNY("CNY", "¥", 1_320);

    private final String code;
    private final String symbol;

    /**
     * Стоимость одной основной единицы валюты
     * в российских копейках.
     *
     * Например:
     * RUB: 1 рубль = 100 копеек
     * USD: 1 доллар = 9600 копеек = 96 рублей
     */
    private final long priceInRublesKopecks;

    Currency(
            String code,
            String symbol,
            long priceInRublesKopecks
    ) {
        this.code = code;
        this.symbol = symbol;
        this.priceInRublesKopecks =
                priceInRublesKopecks;
    }

    public String getCode() {
        return code;
    }

    public String getSymbol() {
        return symbol;
    }

    public long getPriceInRublesKopecks() {
        return priceInRublesKopecks;
    }
}
