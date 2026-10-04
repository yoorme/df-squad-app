package com.yoorme.squadsignup.ui.auth

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.safeDrawingPadding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Button
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.FilterChip
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.unit.dp
import com.yoorme.squadsignup.core.ApiException
import com.yoorme.squadsignup.core.Repo
import com.yoorme.squadsignup.core.SessionStore
import com.yoorme.squadsignup.core.Servers
import com.yoorme.squadsignup.ui.components.PrefixedInput
import kotlinx.coroutines.launch

// 登录页：顶部选择战队（MMR / YFD），输入 昵称 或 前缀+昵称
@Composable
fun LoginScreen(
    store: SessionStore,
    repo: Repo,
    initialServerId: String?,
    onLoggedIn: () -> Unit,
    onGoRegister: () -> Unit,
) {
    var serverId by rememberSaveable { mutableStateOf(initialServerId ?: Servers.ALL.first().id) }
    var username by rememberSaveable { mutableStateOf("") }
    var password by rememberSaveable { mutableStateOf("") }
    var error by remember { mutableStateOf<String?>(null) }
    var loading by remember { mutableStateOf(false) }
    val scope = rememberCoroutineScope()

    val server = Servers.byId(serverId)

    Column(
        modifier = Modifier
            .fillMaxSize()
            .safeDrawingPadding()
            .verticalScroll(rememberScrollState())
            .imePadding()
            .padding(24.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center,
    ) {
        Text("三角洲行动战队管理", style = MaterialTheme.typography.headlineLarge, fontWeight = FontWeight.Bold)
        Spacer(Modifier.height(4.dp))
        Text("战队内部系统", style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant)
        Spacer(Modifier.height(24.dp))

        Text("选择战队", style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant, modifier = Modifier.fillMaxWidth())
        Spacer(Modifier.height(6.dp))
        Row(horizontalArrangement = Arrangement.spacedBy(8.dp), modifier = Modifier.fillMaxWidth()) {
            Servers.ALL.forEach { s ->
                FilterChip(
                    selected = serverId == s.id,
                    onClick = { serverId = s.id },
                    label = { Text(s.label) },
                )
            }
        }
        Spacer(Modifier.height(16.dp))

        PrefixedInput(
            value = username,
            onValueChange = { username = it },
            prefix = server.prefixHint.removeSuffix("丨"),
            placeholder = "请输入昵称",
            modifier = Modifier.fillMaxWidth(),
            enabled = !loading,
        )
        Spacer(Modifier.height(12.dp))
        OutlinedTextField(
            value = password,
            onValueChange = { password = it },
            label = { Text("密码") },
            visualTransformation = PasswordVisualTransformation(),
            modifier = Modifier.fillMaxWidth(),
            singleLine = true,
        )
        error?.let {
            Spacer(Modifier.height(8.dp))
            Text(it, color = MaterialTheme.colorScheme.error, style = MaterialTheme.typography.bodySmall)
        }
        Spacer(Modifier.height(20.dp))

        Button(
            onClick = {
                if (username.isBlank() || password.isBlank()) {
                    error = "请输入昵称和密码"
                    return@Button
                }
                loading = true
                error = null
                scope.launch {
                    try {
                        store.setServer(serverId)
                        val resp = repo.login(username.trim(), password)
                        store.saveLogin(resp.token, resp.user)
                        onLoggedIn()
                    } catch (e: ApiException) {
                        error = if (e.code == 401) "用户不存在或者密码错误" else e.message
                    } catch (e: Exception) {
                        android.util.Log.e("SquadAuth", "login failed", e)
                        error = "网络错误，请稍后重试"
                    } finally {
                        loading = false
                    }
                }
            },
            modifier = Modifier.fillMaxWidth().height(48.dp),
            enabled = !loading,
        ) {
            if (loading) CircularProgressIndicator(modifier = Modifier.height(20.dp)) else Text("登录")
        }
        Spacer(Modifier.height(8.dp))
        TextButton(onClick = onGoRegister) { Text("使用邀请码注册") }
    }
}

// 注册页：邀请码 + 昵称 + 密码（1-16 字符）
@Composable
fun RegisterScreen(
    store: SessionStore,
    repo: Repo,
    initialServerId: String?,
    onRegistered: (String, String, String) -> Unit, // username, nickname, password → 提示后回登录
    onBack: () -> Unit,
) {
    var serverId by rememberSaveable { mutableStateOf(initialServerId ?: Servers.ALL.first().id) }
    var invitationCode by rememberSaveable { mutableStateOf("") }
    var nickname by rememberSaveable { mutableStateOf("") }
    var password by rememberSaveable { mutableStateOf("") }
    var confirm by rememberSaveable { mutableStateOf("") }
    var error by remember { mutableStateOf<String?>(null) }
    var loading by remember { mutableStateOf(false) }
    val scope = rememberCoroutineScope()
    val server = Servers.byId(serverId)

    Column(
        modifier = Modifier
            .fillMaxSize()
            .safeDrawingPadding()
            .verticalScroll(rememberScrollState())
            .imePadding()
            .padding(24.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center,
    ) {
        Text("注册账号", style = MaterialTheme.typography.headlineMedium, fontWeight = FontWeight.Bold)
        Spacer(Modifier.height(4.dp))
        Text("注册后请在登录页登录", style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant)
        Spacer(Modifier.height(20.dp))

        Row(horizontalArrangement = Arrangement.spacedBy(8.dp), modifier = Modifier.fillMaxWidth()) {
            Servers.ALL.forEach { s ->
                FilterChip(selected = serverId == s.id, onClick = { serverId = s.id }, label = { Text(s.label) })
            }
        }
        Spacer(Modifier.height(16.dp))

        OutlinedTextField(
            value = invitationCode, onValueChange = { invitationCode = it },
            label = { Text("邀请码") }, modifier = Modifier.fillMaxWidth(), singleLine = true,
        )
        Spacer(Modifier.height(12.dp))
        PrefixedInput(
            value = nickname,
            onValueChange = { nickname = it },
            prefix = server.prefixHint.removeSuffix("丨"),
            placeholder = "请输入昵称（1-16 个字符）",
            modifier = Modifier.fillMaxWidth(),
            enabled = !loading,
        )
        Spacer(Modifier.height(12.dp))
        OutlinedTextField(
            value = password, onValueChange = { password = it },
            label = { Text("密码（至少 6 位）") },
            visualTransformation = PasswordVisualTransformation(),
            modifier = Modifier.fillMaxWidth(), singleLine = true,
        )
        Spacer(Modifier.height(12.dp))
        OutlinedTextField(
            value = confirm, onValueChange = { confirm = it },
            label = { Text("确认密码") },
            visualTransformation = PasswordVisualTransformation(),
            modifier = Modifier.fillMaxWidth(), singleLine = true,
        )
        error?.let {
            Spacer(Modifier.height(8.dp))
            Text(it, color = MaterialTheme.colorScheme.error, style = MaterialTheme.typography.bodySmall)
        }
        Spacer(Modifier.height(20.dp))

        Button(
            onClick = {
                if (invitationCode.isBlank() || nickname.isBlank() || password.isBlank()) {
                    error = "请填写完整信息"; return@Button
                }
                if (password != confirm) { error = "两次密码不一致"; return@Button }
                loading = true; error = null
                scope.launch {
                    try {
                        store.setServer(serverId)
                        val resp = repo.register(invitationCode.trim(), nickname.trim(), password)
                        onRegistered(resp.username, nickname.trim(), password)
                    } catch (e: ApiException) {
                        error = e.message
                    } catch (e: Exception) {
                        error = "网络错误，请检查网络后重试"
                    } finally {
                        loading = false
                    }
                }
            },
            modifier = Modifier.fillMaxWidth().height(48.dp),
            enabled = !loading,
        ) { Text("注册") }
        Spacer(Modifier.height(8.dp))
        TextButton(onClick = onBack) { Text("返回登录") }
    }
}
