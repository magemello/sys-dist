package org.magemello.sys.node.domain;


import javax.persistence.Entity;
import javax.persistence.Id;

@Entity
public class RecordTerm {

    @Id
    private String key;

    private String value;

    private Integer term;

    private Integer tick;


    public RecordTerm() {
    }

    public RecordTerm(String key, String value, Integer term, Integer tick) {
        this.key = key;
        this.value = value;
        this.term = term;
        this.tick = tick;
    }

    public String getKey() {
        return key;
    }

    public String getValue() {
        return value;
    }

    public Integer getTerm() {
        return term;
    }

    public Integer getTick() {
        return tick;
    }

    @Override
    public String toString() {
        return "RecordTerm{" +
                "key='" + key + '\'' +
                ", value='" + value + '\'' +
                ", term=" + term +
                ", tick=" + tick +
                '}';
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;

        RecordTerm that = (RecordTerm) o;

        if (key != null ? !key.equals(that.key) : that.key != null) return false;
        if (value != null ? !value.equals(that.value) : that.value != null) return false;
        if (term != null ? !term.equals(that.term) : that.term != null) return false;
        return tick != null ? tick.equals(that.tick) : that.tick == null;
    }

    @Override
    public int hashCode() {
        int result = key != null ? key.hashCode() : 0;
        result = 31 * result + (value != null ? value.hashCode() : 0);
        result = 31 * result + (term != null ? term.hashCode() : 0);
        result = 31 * result + (tick != null ? tick.hashCode() : 0);
        return result;
    }
}
