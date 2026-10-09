package net.porplenko.sqlite.cli;

/**
 * Bootstrap placeholder. The real CLI (.tables, SELECT ...) arrives with SJ-7+.
 * You write the storage logic — see BACKLOG.md.
 */
public final class Main {

    public static void main(String[] args) {
        if (args.length == 0) {
            System.out.println("usage: sqlite-java <path-to-db> <command>");
            return;
        }
        throw new UnsupportedOperationException("Not implemented yet — start with SJ-2 (Pager)");
    }
}
