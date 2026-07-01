package iieiiergn.smpAuth.auth

import iieiiergn.smpAuth.common.ClubInfoDto
import iieiiergn.smpAuth.common.StudentData
import team.themoment.datagsm.sdk.oauth.model.ClubInfo
import team.themoment.datagsm.sdk.oauth.model.UserInfo

/** Flattens the DataGSM SDK {@code UserInfo}/{@code Student} into the framework-agnostic {@link StudentData}. */
object StudentMapper {

    fun map(info: UserInfo): StudentData {
        val s = info.student
        return StudentData(
            info.id,
            info.email,
            info.role?.name,
            info.isStudent(),
            s?.name,
            s?.studentNumber,
            s?.grade,
            s?.classNum,
            s?.number,
            s?.sex?.name,
            s?.major?.name,
            s?.role?.name,
            s?.dormitoryFloor,
            s?.dormitoryRoom,
            s?.githubId,
            s?.githubUrl,
            club(s?.majorClub),
            club(s?.autonomousClub),
        )
    }

    private fun club(c: ClubInfo?): ClubInfoDto? {
        if (c == null) return null
        return ClubInfoDto(c.id, c.name, c.type?.name, c.status?.name, c.foundedYear, c.abolishedYear)
    }
}
