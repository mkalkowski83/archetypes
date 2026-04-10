package com.softwarearchetypes.rules.discounting.client;

public enum ClientStatus {
    STANDARD {
        @Override
        public <R> R accept(ClientStatusVisitor<R> visitor) {
            return visitor.visitStandard();
        }
    },
    VIP {
        @Override
        public <R> R accept(ClientStatusVisitor<R> visitor) {
            return visitor.visitVIP();
        }
    },
    GOLD {
        @Override
        public <R> R accept(ClientStatusVisitor<R> visitor) {
            return visitor.visitGold();
        }
    };

    public abstract <R> R accept(ClientStatusVisitor<R> visitor);
}
