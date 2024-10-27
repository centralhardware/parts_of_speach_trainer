import net.sourceforge.jwbf.mediawiki.bots.MediaWikiBot

object Wikitionary {

    private val ignoreTypes = listOf(WordType.PARTICLE)
    private val bot = ThreadLocal<MediaWikiBot>()

    fun isNotValid(text: String, wordType: WordType): Boolean {
        if (bot.get() == null) {
            bot.set(MediaWikiBot("https://ru.wiktionary.org/w/"))
        }

        if (ignoreTypes.contains(wordType)) return false

        val article = bot.get().getArticle(text)
        if (article == null || article.text.isBlank() || article.text.contains("истор.") || article.text.contains("муз.")) return true
        if (article.text.contains("Имя собственное")) {
            return true
        }

        return false
    }


}