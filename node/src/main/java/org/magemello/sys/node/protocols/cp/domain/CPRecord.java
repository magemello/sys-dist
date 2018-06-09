package org.magemello.sys.node.protocols.cp.domain;


import javax.persistence.Entity;

import org.magemello.sys.node.domain.Record;

@Entity
public class CPRecord extends Record{

    private Integer term;

    private Integer tick;

    public CPRecord() {
    }

    public CPRecord(String key, String value, Integer term, Integer tick) {
        super(key, value);
        this.term = term;
        this.tick = tick;
    }

    public Integer getTerm() {
        return term;
    }

    public Integer getTick() {
        return tick;
    }

    @Override
    public String toString() {
        return "{" +
                "key='" + super.getKey() + '\'' +
                ", val='" + super.getVal() + '\'' +
                ", term=" + term +
                ", tick=" + tick +
                '}';
    }

    @Override
    public int hashCode() {
        final int prime = 31;
        int result = super.hashCode();
        result = prime * result + ((term == null) ? 0 : term.hashCode());
        result = prime * result + ((tick == null) ? 0 : tick.hashCode());
        return result;
    }

    @Override
    public boolean equals(Object obj) {
        if (this == obj)
            return true;
        if (!super.equals(obj))
            return false;
        if (getClass() != obj.getClass())
            return false;
        CPRecord other = (CPRecord) obj;
        if (term == null) {
            if (other.term != null)
                return false;
        } else if (!term.equals(other.term))
            return false;
        if (tick == null) {
            if (other.tick != null)
                return false;
        } else if (!tick.equals(other.tick))
            return false;
        return true;
    }

}
