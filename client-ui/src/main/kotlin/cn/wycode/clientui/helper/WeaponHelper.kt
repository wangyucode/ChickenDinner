package cn.wycode.clientui.helper

import org.springframework.stereotype.Component

@Component
class WeaponHelper {
    var weaponNumber = 1

    fun changeWeapon(number: Int) {
        weaponNumber = number
    }
}