package pt.hotelbooking.hotel.config;

import org.springframework.core.convert.converter.Converter;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.oauth2.jwt.Jwt;
import java.util.Collection;
import java.util.ArrayList;
import java.util.List;

public class RoleClaimConverter implements Converter<Jwt, Collection<GrantedAuthority>> {
    @Override
    public Collection<GrantedAuthority> convert(Jwt jwt) {
        List<GrantedAuthority> authorities = new ArrayList<>();

        addAuthorities(authorities, jwt.getClaimAsStringList("roles"));
        addAuthorities(authorities, jwt.getClaimAsStringList("permissions"));

        return authorities;
    }

    private void addAuthorities(List<GrantedAuthority> authorities, List<String> claims) {
        if (claims == null) {
            return;
        }

        claims.stream()
                .map(SimpleGrantedAuthority::new)
                .forEach(authorities::add);
    }
}
