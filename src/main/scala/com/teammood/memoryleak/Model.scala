package com.teammood.memoryleak

import org.joda.time.DateTime

case class TeamMember(
  id: String,
  providerId: String,
  email: String,
  passwordInfo: Option[PasswordInfo],
  firstName: String,
  lastName: String,
  created: DateTime = DateTime.now,
  active: Boolean = true,
  tags: Seq[Tag] = Seq(),
  holidaysTo: Option[DateTime] = None,
  roles: Seq[Role] = Seq(TeamMateRole),
  scheduleConfiguration: ScheduleConfiguration
)

sealed trait Role

object TeamMateRole extends Role
object TeamAdminRole extends Role
object OrganizationAdminRole extends Role

object Roles {

  def fromString(string: String) = string match {
    case "superadmin" ⇒ OrganizationAdminRole
    case "admin"      ⇒ TeamAdminRole
    case _            ⇒ TeamMateRole
  }

  def toString(role: Role) = role match {
    case OrganizationAdminRole ⇒ "superadmin"
    case TeamAdminRole         ⇒ "admin"
    case TeamMateRole          ⇒ "member"
  }

}

case class Tag(name: String)

case class PasswordInfo(
  hasher: String,
  password: String,
  salt: Option[String] = None
)

case class ScheduleConfiguration(
  timeZone: String,
  hour: Int,
  minute: Int,
  monday: Boolean,
  tuesday: Boolean,
  wednesday: Boolean,
  thursday: Boolean,
  friday: Boolean,
  saturday: Boolean,
  sunday: Boolean,
  language: String
)