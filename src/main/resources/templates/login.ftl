<#import "parts/common.ftl" as c>
<#import "parts/login.ftl" as l>

<@c.page>
    <#if Session?? && Session.SPRING_SECURITY_LAST_EXCEPTION??>
        <div class="alert alert-danger" role="alert">
            Username or password is incorrect or email not activated!
        </div>
    </#if>
    <@l.login "/login" false/>
</@c.page>