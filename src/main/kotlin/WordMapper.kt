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
                AND status != 'IGNORE'
            ORDER BY RANDOM()
            LIMIT 1;
        """, mapOf("difficult" to difficult.name))
            .map { row -> Pair(row.string("written_rep"), WordType.fromCode(row.string("part_of_speech"))) }.asSingle
    )!!

    fun markWord(word: String, status: WordStatus) = session.execute(queryOf(
        """
        UPDATE entry SET status = :status WHERE written_rep = :word
    """, mapOf("word" to word, "status" to status.name)
    ))

    fun isNotValid(word: String) = session.run(queryOf(
        """
           SELECT status != 'APPROVED' as valid FROM entry WHERE written_rep = :word  
        """, mapOf("word" to word)
    ).map{ row -> row.boolean("valid") }.asSingle)!!

}