package com.github.ruediste1.btrbck.dto;

import java.io.Serializable;

public class Block implements Serializable {
    private static final long serialVersionUID = 1L;

    public boolean isLast;
    public boolean isFirst;
    public byte[] data;
}
