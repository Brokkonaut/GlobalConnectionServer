package de.cubeside.globalserver;

import java.text.ParseException;
import java.util.Iterator;
import java.util.NoSuchElementException;

public class ArgsParser implements Iterable<String>, Iterator<String> {
    private String[] args;

    private int current;

    public ArgsParser(String[] args) {
        this.args = args;
        this.current = -1;
    }

    public ArgsParser(String[] args, int skipParts) {
        this.args = args;
        this.current = -1 + skipParts;
    }

    @Override
    public boolean hasNext() {
        return current < args.length - 1;
    }

    public int remaining() {
        return Math.max(args.length - 1 - current, 0);
    }

    public String getAll(String def) {
        ++current;
        if (args.length <= current) {
            return def;
        }
        StringBuilder sb = new StringBuilder();
        while (args.length > current) {
            sb.append(args[current]);
            sb.append(' ');
            ++current;
        }
        if (sb.length() > 0) {
            sb.deleteCharAt(sb.length() - 1);
        }
        return sb.toString();
    }

    public String seeNext(String def) {
        if (args.length <= current + 1) {
            return def;
        }
        return args[current + 1];
    }

    public String getNext(String def) {
        ++current;
        if (args.length <= current) {
            return def;
        }
        return args[current];
    }

    @Override
    public String next() {
        return getNext();
    }

    public String getNext() {
        String res = getNext(null);
        if (res == null) {
            throw new NoSuchElementException();
        }
        return res;
    }

    public int getNext(int def) {
        String next = getNext(null);
        if (next == null) {
            return def;
        }
        try {
            return Integer.parseInt(next);
        } catch (NumberFormatException e) {
            return def;
        }
    }

    public double getNext(double def) {
        String next = getNext(null);
        if (next == null) {
            return def;
        }
        try {
            return Double.parseDouble(next);
        } catch (NumberFormatException e) {
            return def;
        }
    }

    public Boolean getNext(boolean ignored) {
        String next = getNext(null);
        if (next == null) {
            return null;
        }
        if (next.equalsIgnoreCase("true")) {
            return Boolean.TRUE;
        } else if (next.equalsIgnoreCase("false")) {
            return Boolean.FALSE;
        }
        return null;
    }

    public long getNextTimespan() throws NumberFormatException, ParseException {
        String string = getNext();
        string = string.toLowerCase();
        long res = 0;
        if (string.endsWith("s")) {
            res += Integer.parseInt(string.substring(0, string.length() - 1)) * 1000;
        } else if (string.endsWith("m")) {
            res += Integer.parseInt(string.substring(0, string.length() - 1)) * 1000 * 60;
        } else if (string.endsWith("h")) {
            res += Integer.parseInt(string.substring(0, string.length() - 1)) * 1000 * 60 * 60;
        } else if (string.endsWith("d")) {
            res += Integer.parseInt(string.substring(0, string.length() - 1)) * 1000 * 60 * 60 * 24;
        } else {
            throw new ParseException("String doesn't end with s, m h or d", string.length() - 1);
        }
        return res;
    }

    public long getAllTimespan() throws NumberFormatException, ParseException {
        long res = 0;
        while (hasNext()) {
            res += getNextTimespan();
        }
        return res;
    }

    @Override
    public Iterator<String> iterator() {
        return clone();
    }

    @Override
    public ArgsParser clone() {
        ArgsParser result = new ArgsParser(args);
        result.current = this.current;
        return result;
    }
}
