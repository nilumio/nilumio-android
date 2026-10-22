package com.github.damontecres.wholphin.services

import android.net.Uri
import com.github.damontecres.wholphin.ui.setup.FriendInviteViewModel
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

@RunWith(RobolectricTestRunner::class)
@Config(manifest = Config.NONE, sdk = [30])
class FriendInviteTest {
    private val invite = FriendInvite("https://jf.example.org", "Anna", "pä\$s/+?#=ü", "Wohnzimmer")

    @Test
    fun roundTrip() {
        val link = invite.toLink()
        assertTrue(link.startsWith("https://nilumio.com/einladung#"))
        assertEquals(invite, FriendInvite.fromUri(Uri.parse(link)))
        val custom = link.replace("https://nilumio.com/einladung", "nilumio://einladung")
        assertEquals(invite, FriendInvite.fromUri(Uri.parse(custom)))
    }

    @Test
    fun rejectsOtherLinks() {
        assertNull(FriendInvite.fromUri(Uri.parse("https://example.com/einladung#abc")))
        assertNull(FriendInvite.fromUri(Uri.parse("nilumio://einladung#!!!")))
        assertFalse(invite.toString().contains(invite.p))
    }

    @Test
    fun privateAddresses() {
        assertTrue(FriendInviteViewModel.isPrivateAddress("http://192.168.178.20:8096"))
        assertTrue(FriendInviteViewModel.isPrivateAddress("http://172.20.0.2:8096"))
        assertTrue(FriendInviteViewModel.isPrivateAddress("jellyfin.local:8096"))
        assertFalse(FriendInviteViewModel.isPrivateAddress("https://jf.example.org"))
        assertFalse(FriendInviteViewModel.isPrivateAddress("http://172.40.1.1"))
    }
}
