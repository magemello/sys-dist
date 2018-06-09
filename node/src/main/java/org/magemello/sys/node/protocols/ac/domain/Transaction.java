package org.magemello.sys.node.protocols.ac.domain;

public class Transaction {

    private String _ID;

    private String key;

    private String value;

    public Transaction() {
    }

    public Transaction(String key, String value) {
        this._ID = Long.toHexString(System.currentTimeMillis());
        this.key = key;
        this.value = value;
    }

    public String get_ID() {
        return _ID;
    }

    public String getKey() {
        return key;
    }

    public String getValue() {
        return value;
    }

    @Override
    public String toString() {
        return "Transaction{" +
                "_ID='" + _ID + '\'' +
                ", key='" + key + '\'' +
                ", value='" + value + '\'' +
                '}';
    }
}
