import kotliquery.queryOf
import kotliquery.sessionOf

object WordMapper {

    val session = sessionOf(System.getenv("POSTGRES_URL"),
        System.getenv("POSTGRES_USERNAME"),
        System.getenv("POSTGRES_PASSWORD"))
    fun getRandomWord(difficult: Difficult): Pair<String, WordType> = session.run(
        queryOf("""
            WITH RandomType AS (
                SELECT part_of_speech as type
                FROM entry
                WHERE (CASE 
                        WHEN :difficult = 'EASY' THEN part_of_speech IN ('noun', 'adjective', 'verb')
                        WHEN :difficult = 'MEDIUM' THEN part_of_speech IN ('noun', 'adjective', 'verb', 'adverb', 'number', 'pronoun', 'conjunction', 'preposition', 'particle', 'interjection')
                        WHEN :difficult = 'HARd' THEN part_of_speech IN ('noun', 'adjective', 'verb', 'adverb', 'number', 'pronoun', 'conjunction', 'preposition', 'particle', 'interjection', 'participle', 'participleAdjective')
                END)
                GROUP BY part_of_speech
                ORDER BY RANDOM()
                LIMIT 1
            )
            SELECT written_rep, part_of_speech
            FROM entry
            WHERE part_of_speech = (SELECT type FROM RandomType) 
                AND (
                CASE 
                        WHEN :difficult = 'EASY' THEN part_of_speech IN ('noun', 'adjective', 'verb')
                        WHEN :difficult = 'MEDIUM' THEN part_of_speech IN ('noun', 'adjective', 'verb', 'adverb', 'number', 'pronoun', 'conjunction', 'preposition', 'particle', 'interjection')
                        WHEN :difficult = 'HARD' THEN part_of_speech IN ('noun', 'adjective', 'verb', 'adverb', 'number', 'pronoun', 'conjunction', 'preposition', 'particle', 'interjection', 'participle', 'participleAdjective')
                END
                )
                AND written_rep NOT IN (SELECT word FROM ignore_words)
                AND written_rep NOT IN (SELECT word FROM swear)
            ORDER BY RANDOM()
            LIMIT 1;
        """, mapOf("difficult" to difficult.name))
            .map { row -> Pair(row.string("written_rep"), WordType.fromCode(row.string("part_of_speech"))) }.asSingle
    )!!

    fun addToIgnore(word: String) = session.execute(queryOf(
        """
        INSERT INTO ignore_words (word) VALUES (:word)
    """, mapOf("word" to word)
    ))

}