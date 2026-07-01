package iieiiergn.smpAuth.common;

/**
 * Club membership, mirrored from the DataGSM SDK {@code ClubInfo} model.
 * May be {@code null} on a {@link StudentData} when the student has no such club.
 */
public record ClubInfoDto(
        Long id,
        String name,
        String type,
        String status,
        Integer foundedYear,
        Integer abolishedYear
) {
}
