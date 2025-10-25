package dev.cypphi.mcrc.discord.util;

import java.util.ArrayList;
import java.util.List;
import java.util.Objects;

public final class DiscordMessageSpec {
    private final String title;
    private final String description;
    private final DiscordMessageKind kind;
    private final String footer;
    private final boolean timestamp;
    private final String thumbnailUrl;
    private final String imageUrl;
    private final List<Field> fields;

    private DiscordMessageSpec(Builder builder) {
        this.title = builder.title;
        this.description = builder.description;
        this.kind = builder.kind;
        this.footer = builder.footer;
        this.timestamp = builder.timestamp;
        this.thumbnailUrl = builder.thumbnailUrl;
        this.imageUrl = builder.imageUrl;
        this.fields = List.copyOf(builder.fields);
    }

    public String title() {
        return title;
    }

    public String description() {
        return description;
    }

    public DiscordMessageKind kind() {
        return kind;
    }

    public String footer() {
        return footer;
    }

    public boolean timestamp() {
        return timestamp;
    }

    public String thumbnailUrl() {
        return thumbnailUrl;
    }

    public String imageUrl() {
        return imageUrl;
    }

    public List<Field> fields() {
        return fields;
    }

    public static Builder builder() {
        return new Builder();
    }

    public static DiscordMessageSpec of(String description, DiscordMessageKind kind) {
        return builder()
                .description(description)
                .kind(kind)
                .build();
    }

    public static final class Builder {
        private String title;
        private String description;
        private DiscordMessageKind kind = DiscordMessageKind.INFO;
        private String footer;
        private boolean timestamp;
        private String thumbnailUrl;
        private String imageUrl;
        private final List<Field> fields = new ArrayList<>();

        private Builder() {}

        public Builder title(String title) {
            this.title = title;
            return this;
        }

        public Builder description(String description) {
            this.description = description;
            return this;
        }

        public Builder kind(DiscordMessageKind kind) {
            if (kind != null) {
                this.kind = kind;
            }
            return this;
        }

        public Builder footer(String footer) {
            this.footer = footer;
            return this;
        }

        public Builder timestamp(boolean timestamp) {
            this.timestamp = timestamp;
            return this;
        }

        public Builder thumbnailUrl(String thumbnailUrl) {
            this.thumbnailUrl = thumbnailUrl;
            return this;
        }

        public Builder imageUrl(String imageUrl) {
            this.imageUrl = imageUrl;
            return this;
        }

        public Builder addField(String name, String value, boolean inline) {
            if (name != null && value != null) {
                fields.add(new Field(name, value, inline));
            }
            return this;
        }

        public DiscordMessageSpec build() {
            Objects.requireNonNull(description, "description must not be null");
            return new DiscordMessageSpec(this);
        }
    }

    public record Field(String name, String value, boolean inline) {}
}
