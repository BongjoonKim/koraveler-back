package server.nadeliv.blog.constants;

public class BlogConstants {
    public enum BlogPageType {
        HOME("home"),
        MY_BLOG("my-blog"),
        BOOKMARK("bookmark"),
        DRAFT("draft"),
        HIDDEN("hidden");

        private final String value;

        BlogPageType(String value) {
            this.value = value;
        }

        public String getValue() {
            return value;
        }
    }

}
