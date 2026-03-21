package com.example.features.masters

import com.example.common.enums.EmploymentStatus
import kotlinx.serialization.Serializable

@Serializable
data class MasterResponse(
    val id: Long,
    val fullName: String,
    val specialization: String?,
    val experienceYears: Int?,
    val phone: String?,
    val email: String?,
    val employmentStatus: EmploymentStatus
)

@Serializable
data class CreateMasterRequest(
    val fullName: String,
    val specialization: String? = null,
    val experienceYears: Int? = null,
    val phone: String? = null,
    val email: String? = null,
    val employmentStatus: EmploymentStatus = EmploymentStatus.ACTIVE
)

@Serializable
data class UpdateMasterRequest(
    val fullName: String,
    val specialization: String? = null,
    val experienceYears: Int? = null,
    val phone: String? = null,
    val email: String? = null,
    val employmentStatus: EmploymentStatus
)