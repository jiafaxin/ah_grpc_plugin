package com.autohome.ah_grpc_plugin.services;

import com.autohome.ah_grpc_plugin.Config;
import com.autohome.ah_grpc_plugin.dialogs.GitUserLoginDialog;
import com.autohome.ah_grpc_plugin.dialogs.NoUserDialog;
import com.autohome.ah_grpc_plugin.enums.ResultCode;
import com.autohome.ah_grpc_plugin.models.*;
import com.autohome.ah_grpc_plugin.models.gitlab.*;
import com.autohome.ah_grpc_plugin.ui.MainToolwindow;
import com.autohome.ah_grpc_plugin.utils.HttpClientUtil;
import com.autohome.ah_grpc_plugin.utils.JsonUtils;
import com.autohome.ah_grpc_plugin.utils.UrlUtils;
import com.fasterxml.jackson.core.type.TypeReference;
import com.google.gson.JsonObject;
import com.intellij.find.impl.FindPopupPanel;
import com.intellij.openapi.project.Project;
import io.netty.handler.codec.http.DefaultHttpHeaders;
import io.netty.handler.codec.http.HttpHeaders;
import org.apache.commons.lang3.StringUtils;

import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.text.SimpleDateFormat;
import java.util.*;
import java.util.concurrent.CompletableFuture;

/**
 * crated by shicuining 2023/1/2
 * git lab 操作类
 */
public class GitlabApiService {
    final static String refresh_key = "git_refresh_key";

    final static String token_key = "git_token_key";

    final static String current_user_key = "git_current_user_key";

    final static String read_token = "gdLmW8s7LJGjy9j5HgqS";

    final static String baseUrl = "https://git-ops.corpautohome.com";


    public static CompletableFuture<ApiResult<CreateFileResult>> createFile(Project project, String path,String content,String commitMessage){
        NewFileParam param = new NewFileParam();
        param.setBranch("master");
        param.setCommit_message(commitMessage);
        param.setContent(content);
        CompletableFuture<ApiResult<CreateFileResult>> newFile = HttpClientUtil.postJson(getRealUrl("/repository/files/" + UrlUtils.encode(path)), JsonUtils.toString(param), new TypeReference<CreateFileResult>() {
        },getHeaders(project),5000);
        return newFile;
    }


    public static CompletableFuture<ApiResult<CreateFileResult>> updateFile(Project project, String path,String content,String commitMessage){
        NewFileParam param = new NewFileParam();
        param.setBranch("master");
        param.setCommit_message(commitMessage);
        param.setContent(content);
        CompletableFuture<ApiResult<CreateFileResult>> newFile = HttpClientUtil.put(getRealUrl("/repository/files/" + UrlUtils.encode(path)), JsonUtils.toString(param), new TypeReference<CreateFileResult>() {
        },getHeaders(project),5000);
        return newFile;
    }


    public static ApiResult<List<TreeItem>> getItems(Project project,String path) {
        return getItems(project,path, false, 1);
    }

    /**
     * 根据path查询git目录
     *
     * @param path
     * @return
     */
    public static ApiResult<List<TreeItem>> getItems(Project project, String path, boolean recursive, int page) {
        ApiResult<List<TreeItem>> result = get(project,"/repository/tree?per_page=100&page=" + page + "&path=" + path + "&recursive=" + recursive, new TypeReference<List<TreeItem>>() {
        }).join();
        if (result.getCode() >= 300) {
            return result;
        }
        //移除非proto的结点
        result.getResult().removeIf(r -> r.getType().equals("blob") && r.getName().indexOf(".proto") <= 0);
        return result;
    }

    /**
     * 根据path，获取文件详情
     *
     * @param path
     * @return
     */
    public static String getContent(Project project, String path) {
        FileDetail fileDetail = getFile(project,path);
        if (fileDetail == null || fileDetail.getContent() == null)
            return "";
        return fileDetail.getContent();
    }

    public static CompletableFuture<FileDetail> getFileAsync(Project project,String path) {
        if (path == null)
            return CompletableFuture.completedFuture(null);
        return get(project,"/repository/files/" + UrlUtils.encode(path) + "?ref=master", new TypeReference<FileDetail>() {
        }).thenApply(result -> {
            if (result.getCode() < 300)
                return result.getResult();
            return null;
        });
    }

    public static FileDetail getFile(Project project,String path) {
        ApiResult<FileDetail> result = get(project, "/repository/files/" + UrlUtils.encode(path) + "?ref=master", new TypeReference<FileDetail>() {
        }).join();
        if (result.getCode() < 300) {
            if (result.getResult() != null && StringUtils.isNotBlank(result.getResult().getContent())) {
                result.getResult().setContent(new String(Base64.getDecoder().decode(result.getResult().getContent().getBytes(StandardCharsets.UTF_8)), StandardCharsets.UTF_8));
            }
            return result.getResult();
        }
        return null;
    }

    public static CompletableFuture<List<BlameItem>> getFileBlames(Project project, String path) {
        if (path == null)
            return CompletableFuture.completedFuture(new ArrayList<>());
        return get(project,"/repository/files/" + UrlUtils.encode(path) + "/blame?ref=master", new TypeReference<List<BlameItem>>() {
        }).thenApply(result -> {
            if (result.getCode() < 300)
                return result.getResult();
            return new ArrayList<>();
        });
    }

    public static List<SearchResult> search(Project project, String keyword) {
        ApiResult<List<SearchResult>> list = get(project,"/search?scope=blobs&search=" + URLEncoder.encode(keyword), new TypeReference<List<SearchResult>>() {
        }).join();
        if (list.getCode() < 300)
            return list.getResult();
        return new ArrayList<>();
    }

    public static boolean loginIfNot(Project project) {
        if (!hasLogin()) {
            return login(project) != null;
        }
        return true;
    }

    public static CompletableFuture<ApiResult<GitCommitResult>> commit(Project project, GitPushList pushList) {
        return HttpClientUtil.postJson(
                getRealUrl("/repository/commits"),
                JsonUtils.toString(pushList),
                new TypeReference<GitCommitResult>() {
                },
                getHeaders(project),
                100000
        );
    }

    public static <T> CompletableFuture<ApiResult<T>> get(Project project, String url, TypeReference<T> typeReference) {
        Map<String, String> headerParam = new HashMap<>();
        headerParam.put("PRIVATE-TOKEN",read_token);
        return HttpClientUtil.get(
                url.startsWith("http")? url : getRealUrl(url),
                typeReference,
                headerParam,
                100000,
                "UTF-8"
        );
    }


    static String getRealUrl(String apiPath) {
        return baseUrl + "/api/v4/projects/" + Config.gitProjectId + apiPath;
    }

    static HttpHeaders getHeaders(Project project) {
        HttpHeaders headers = new DefaultHttpHeaders();
        headers.add("Authorization", "Bearer " + getToken(project));
        return headers;
    }

    public static MethodResult login(Project project, String userName, String password) {
        JsonObject jsonObject = new JsonObject();
        jsonObject.addProperty("grant_type", "password");
        jsonObject.addProperty("username", userName);
        jsonObject.addProperty("password", password);
        ApiResult<GitUserResult> gitUserResult = HttpClientUtil.postJson(
                baseUrl + "/oauth/token",
                jsonObject.toString(),
                new TypeReference<GitUserResult>() {
                },
                new DefaultHttpHeaders(),
                100000
        ).join();

        if (gitUserResult.getCode() > 300) {
            if (gitUserResult.getCode() == 400 && gitUserResult.getResult().getError().equals("invalid_grant")) {
                //未授权用户，提示先通过web登录一下
                if (new NoUserDialog(project).showAndGet()) {
                    return login(project, userName, password);
                }else{
                    return MethodResult.fail(ResultCode.USERNAME_OR_PASSWORD_ERROR);
                }
            } else {
                if (gitUserResult.getResult() != null && gitUserResult.getResult().getError() != null && gitUserResult.getResult().getError().equals("invalid_grant")) {
                    return MethodResult.fail(ResultCode.USERNAME_OR_PASSWORD_ERROR);
                }
                return MethodResult.fail(ResultCode.ERROR, gitUserResult.getMsg());
            }
        }

        saveToken(gitUserResult.getResult().getRefresh_token(), gitUserResult.getResult().getAccess_token());
        saveUserName(userName);

        //自动将当前用户加入大仓
        addCurrentUserToIDL(project,userName);

        return MethodResult.success();
    }

    public static boolean refreshTokenSuccess(Project project) {
        return StringUtils.isNotBlank(refreshToken(project));
    }

    public static String refreshToken(Project project) {
        DbService dbService = new DbService();
        String refresh_token = dbService.get(refresh_key);
        if (StringUtils.isBlank(refresh_token)) {
            //没有refresh_token，则刷新
            return login(project);
        }
        JsonObject jsonObject = new JsonObject();
        jsonObject.addProperty("grant_type", "refresh_token");
        jsonObject.addProperty("refresh_token", refresh_token);
        ApiResult<GitUserResult> gitUserResult = HttpClientUtil.postJson(
                baseUrl + "/oauth/token",
                jsonObject.toString(),
                new TypeReference<GitUserResult>() {
                },
                new DefaultHttpHeaders(),
                100000
        ).exceptionally(e -> {
            return new ApiResult<>() {
                {
                    setCode(500);
                }
            };
        }).join();
        if (gitUserResult.getCode() == 0) {
            saveToken(gitUserResult.getResult().getRefresh_token(), gitUserResult.getResult().getAccess_token());
            return gitUserResult.getResult().getAccess_token();
        }
        return login(project);
    }


    public static boolean hasLogin() {
        DbService dbService = new DbService();
        String token = dbService.get(token_key);
        if (StringUtils.isNotBlank(token)) {
            return true;
        }
        return false;
    }

    public static String getCurrentUser(){
        if(!hasLogin())
            return "";
        DbService dbService = new DbService();
        return dbService.get(current_user_key);
    }

    public static String login(Project project) {
        GitUserLoginDialog gitUserLoginDialog = new GitUserLoginDialog(project);
        if (gitUserLoginDialog.showAndGet()) {
            DbService dbService = new DbService();
            String token = dbService.get(token_key);
            MainToolwindow.getInstance(project).login();
            return token;
        } else {
            return null;
        }
    }

    public static boolean logout() {
        DbService dbService = new DbService();
        dbService.set(token_key, null);
        dbService.set(refresh_key, null);

        return true;
    }


    public static void saveToken(String refresh_token, String access_token) {
        DbService dbService = new DbService();
        dbService.set(refresh_key, refresh_token);
        dbService.set(token_key, access_token);
    }

    public static void saveUserName(String userName) {
        DbService dbService = new DbService();
        dbService.set(current_user_key, userName);
    }


    /**
     * 获取token，后期需要用户登录
     *
     * @return
     */
    static String getToken(Project project) {
        DbService dbService = new DbService();
        String token = dbService.get(token_key);
        if (!StringUtils.isBlank(token)) {
            return token;
        }
        return refreshToken(project);
    }

    public static void removeToken(int type) {
        DbService dbService = new DbService();
        switch (type) {
            case 1:
                dbService.set(token_key, null);
                dbService.set(refresh_key, null);
                break;
            case 2:
                dbService.set(token_key, null);
                break;
            case 3:
                dbService.set(token_key, "123");
                dbService.set(refresh_key, "123");
                break;
            case 4:
                dbService.set(token_key, "123");
                break;
        }
    }

    public static CompletableFuture<ApiResult<CreateBranchResult>> createBranches(Project project) {
        String newName = new SimpleDateFormat("yyyyMMddHHmmss").format(new Date());
        return HttpClientUtil.postJson(
                getRealUrl("/repository/branches?branch=auto_" + newName + "&ref=master"),
                "",
                new TypeReference<CreateBranchResult>() {
                },
                getHeaders(project),
                100000
        );
    }

    public static CompletableFuture<ApiResult<MergeRequestResult>> mergeRequests(Project project, String branchName) {
        String newName = new SimpleDateFormat("yyyyMMddHHmmss").format(new Date());
        return HttpClientUtil.postJson(
                getRealUrl("/merge_requests?source_branch=" + branchName + "&target_branch=master&title=test"),
                "",
                new TypeReference<MergeRequestResult>() {
                },
                getHeaders(project),
                100000
        );
    }

    public static CompletableFuture<ApiResult<MergeResult>> merge(Project project, Integer iid) {
        String newName = new SimpleDateFormat("yyyyMMddHHmmss").format(new Date());
        return HttpClientUtil.put(
                getRealUrl("/merge_requests/"+iid+"/merge"),
                "",
                new TypeReference<MergeResult>() {
                },
                getHeaders(project),
                100000
        );
    }


    public static UserInfo getUserInfo(Project project,String userName){
        ApiResult<List<UserInfo>> userInfo = get(project, baseUrl.concat("/api/v4/users?username=").concat(userName), new TypeReference<List<UserInfo>>() {}).join();
        if(userInfo==null||userInfo.getCode()>=400 || userInfo.getResult()==null||userInfo.getResult().size()==0){
            return null;
        }
        return userInfo.getResult().get(0);
    }


    public static UserInfo getProjectUserInfo(Project project,int userId) {

        ApiResult<UserInfo> userInfo = get(project, getRealUrl("/members/all/").concat(userId + ""), new TypeReference<UserInfo>() {
        }).join();
        if (userInfo == null || userInfo.getCode() >= 400 || userInfo.getResult() == null) {
            return null;
        }
        return userInfo.getResult();
    }

    public static boolean addCurrentUserToIDL(Project project,String userName) {
        UserInfo baseUserInfo = getUserInfo(project,userName);
        if (baseUserInfo == null)
            return false;
        UserInfo projectUserInfo = getProjectUserInfo(project, baseUserInfo.getId());
        if (projectUserInfo != null)
            return true;

        HttpHeaders headers = new DefaultHttpHeaders();
        headers.add("PRIVATE-TOKEN", read_token);

        ApiResult<UserInfo> newUserInfo = HttpClientUtil.postJson(getRealUrl("/members"), "{\"user_id\":"+baseUserInfo.getId()+",\"access_level\":40}", new TypeReference<UserInfo>() {
        }, headers, 5000).join();

        if (newUserInfo == null)
            return false;

        if (newUserInfo.getCode() == 409)
            return true;

        if (newUserInfo.getCode() > 400) {
            NotifyService.error(project,"自动加入IDL失败["+newUserInfo.getCode()+"]，请联系相关开发人员！！！");
            return false;
        }

        if (newUserInfo.getResult() != null && newUserInfo.getResult().getId() > 0)
            return true;

        NotifyService.error(project,"自动加入IDL失败，请联系相关开发人员！！！");
        return false;
    }



}
