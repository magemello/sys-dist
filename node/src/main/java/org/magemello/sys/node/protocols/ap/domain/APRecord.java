package org.magemello.sys.node.protocols.ap.domain;


import javax.persistence.Entity;

import org.magemello.sys.node.domain.Record;

@Entity
public class APRecord extends Record {
    private Long timestamp;
    
    public APRecord() {
    }

    public APRecord(String key, String value) {
        this(key, value, System.currentTimeMillis());
    }

    public APRecord(String key, String value, Long timestamp) {
        super(key, value);
        this.timestamp = timestamp;
    }

    public Long getTimestamp() {
        return timestamp;
    }

    @Override
    public String toString() {
        return "{" +
                "key='" + super.getKey() + '\'' +
                ", val='" + super.getValue() + '\'' +
                ", ts=" + timestamp+
                '}';
    }

    @Override
    public int hashCode() {
        final int prime = 31;
        int result = super.hashCode();
        result = prime * result + ((timestamp == null) ? 0 : timestamp.hashCode());
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
        APRecord other = (APRecord) obj;
        if (timestamp == null) {
            if (other.timestamp!= null)
                return false;
        } else if (!timestamp.equals(other.timestamp))
            return false;
        return true;
    }

}
