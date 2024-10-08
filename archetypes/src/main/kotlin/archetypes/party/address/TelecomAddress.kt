package archetypes.party.address

import java.time.OffsetDateTime

class TelecomAddress(
    val countryCode: String,
    val nationalDirectDialingPrefix: String,
    val areaCode: String,
    val number: String,
    val extension: String,
    val physicalType: PhysicalType,
    override val validFrom: OffsetDateTime? = null,
    override val validTo: OffsetDateTime? = null,
) : Address() {
    override fun getAddress(): String =
        "$countryCode $nationalDirectDialingPrefix $areaCode $number ext. $extension"
}
