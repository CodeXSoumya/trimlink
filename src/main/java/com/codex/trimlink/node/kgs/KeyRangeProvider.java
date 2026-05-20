package com.codex.trimlink.node.kgs;

public interface KeyRangeProvider {
    /**
    * Fetches the next available unique sequence number.
    * If the current block runs out, it blocks momentarily to lease a new range. 
    */
   long getNextId();
}
