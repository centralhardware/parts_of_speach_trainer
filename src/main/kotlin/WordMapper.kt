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
                WHERE (:difficult = 'EASY' AND part_of_speech IN ('NOUN', 'ADJECTIVE', 'VERB'))
                    OR (:difficult = 'MEDIUM' AND part_of_speech IN ('NOUN', 'ADJECTIVE', 'VERB', 'ADVERB', 'PRONOUN', 'CONJUNCTION', 'PREPOSITION', 'PARTICLE', 'INTERJECTION'))
                    OR (:difficult = 'HARD' AND part_of_speech IN ('NOUN', 'ADJECTIVE', 'VERB', 'ADVERB', 'PRONOUN', 'CONJUNCTION', 'PREPOSITION', 'PARTICLE', 'INTERJECTION', 'PARTICIPLE', 'PARTICIPLE_ADJECTIVE'))
                GROUP BY part_of_speech
                ORDER BY RANDOM()
                LIMIT 1
            )
            SELECT written_rep, part_of_speech
            FROM entry
            WHERE part_of_speech = (SELECT type FROM RandomType) 
                AND ((:difficult = 'EASY' AND part_of_speech IN ('NOUN', 'ADJECTIVE', 'VERB'))
                    OR (:difficult = 'MEDIUM' AND part_of_speech IN ('NOUN', 'ADJECTIVE', 'VERB', 'ADVERB', 'PRONOUN', 'CONJUNCTION', 'PREPOSITION', 'PARTICLE', 'INTERJECTION'))
                    OR (:difficult = 'HARD' AND part_of_speech IN ('NOUN', 'ADJECTIVE', 'VERB', 'ADVERB', 'PRONOUN', 'CONJUNCTION', 'PREPOSITION', 'PARTICLE', 'INTERJECTION', 'PARTICIPLE', 'PARTICIPLE_ADJECTIVE')))
                AND status != 'IGNORE'
            ORDER BY RANDOM()
            LIMIT 1;
        """, mapOf("difficult" to difficult.name))
            .map { row -> Pair(row.string("written_rep"), WordType.valueOf(row.string("part_of_speech"))) }.asSingle
    )!!

    fun markWord(word: String, status: WordStatus, statusReason: IgnoreReason = IgnoreReason.NONE) = session.execute(queryOf(
        """
        UPDATE entry 
        SET status = :status,
            statusreason = :statusReason,
            statusdate = now()
        WHERE written_rep = :word
    """, mapOf("word" to word, "status" to status.name, "statusReason" to statusReason.name)
    ))

    fun isNotValid(word: String) = session.run(queryOf(
        """
           SELECT status != 'APPROVED' as valid FROM entry WHERE written_rep = :word  
        """, mapOf("word" to word)
    ).map{ row -> row.boolean("valid") }.asSingle)!!

}