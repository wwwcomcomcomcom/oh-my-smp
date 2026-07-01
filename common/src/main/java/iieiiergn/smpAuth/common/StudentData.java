package iieiiergn.smpAuth.common;

/**
 * The enrichment payload that travels across the whole system:
 * auth-server REST responses, Velocity state, and the plugin-messaging channel.
 *
 * <p>It is a flattened, framework-agnostic snapshot of the DataGSM SDK
 * {@code UserInfo} + {@code Student} models. Enum fields are stored as their
 * {@code name()} strings so no SDK type leaks into {@code common}.
 *
 * <p>Student-specific fields are {@code null} when {@link #isStudent()} is false.
 */
public record StudentData(
        // From UserInfo
        Long datagsmId,
        String email,
        String role,
        boolean isStudent,
        // From Student
        String name,
        Integer studentNumber,
        Integer grade,
        Integer classNum,
        Integer number,
        String sex,
        String major,
        String studentRole,
        Integer dormitoryFloor,
        Integer dormitoryRoom,
        String githubId,
        String githubUrl,
        ClubInfoDto majorClub,
        ClubInfoDto autonomousClub
) {
}
