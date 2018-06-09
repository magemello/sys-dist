package org.magemello.sys.node.domain;


import javax.persistence.Entity;
import javax.persistence.Id;

@Entity
public class Record {

    @Id
    private String key;

    private String val;

    public Record() {
    }

    public Record(String key, String value) {
        this.key = key;
        this.val = value;
    }

    public String getKey() {
        return key;
    }

    public String getVal() {
        return val;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;

        Record record = (Record) o;

        if (key != null ? !key.equals(record.key) : record.key != null) return false;
        return val != null ? val.equals(record.val) : record.val == null;
    }

    @Override
    public int hashCode() {
        int result = key != null ? key.hashCode() : 0;
        result = 31 * result + (val != null ? val.hashCode() : 0);
        return result;
    }

    @Override
    public String toString() {
        return "Record{" +
                ", key='" + key + '\'' +
                ", value='" + val + '\'' +
                '}';
    }
}
