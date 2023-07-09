package com.crossfit.server.service;

import com.crossfit.server.dto.member.LoginRequestDto;
import com.crossfit.server.dto.member.LoginResponseDto;
import com.crossfit.server.dto.member.MemberDto;
import com.crossfit.server.entity.Authority;
import com.crossfit.server.entity.Member;
import com.crossfit.server.repository.MemberRepository;
import com.crossfit.server.config.jwt.JwtTokenProvider;
import lombok.RequiredArgsConstructor;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.config.annotation.authentication.builders.AuthenticationManagerBuilder;
import org.springframework.security.core.Authentication;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.HashSet;
import java.util.Optional;
import java.util.Set;

@Service
@Transactional(readOnly = true)
@RequiredArgsConstructor
public class MemberService {

    private final MemberRepository memberRepository;
    private final AuthenticationManagerBuilder authenticationManagerBuilder;
    private final JwtTokenProvider jwtTokenProvider;
    private final PasswordEncoder passwordEncoder;

    @Transactional
    public LoginResponseDto login(LoginRequestDto dto) {
        String email = dto.getEmail();
        String password = dto.getPassword();

        UsernamePasswordAuthenticationToken authToken = new UsernamePasswordAuthenticationToken(email, password);

        Authentication auth = authenticationManagerBuilder.getObject().authenticate(authToken);

        LoginResponseDto loginResponseDto = jwtTokenProvider.generateToken(auth);

        Member member = memberRepository.findByEmail(dto.getEmail())
                .orElseThrow(()->new RuntimeException());
        member.updateToken(email, loginResponseDto.getRefreshToken());
        memberRepository.save(member);

        return loginResponseDto;
    }

    @Transactional
    public Member register(MemberDto dto) {
        Set<Authority> authorities = new HashSet<>();
        Authority userAuthority = new Authority("ROLE_USER");
        authorities.add(userAuthority);

        Optional<Member> existingMember = memberRepository.findByEmail(dto.getEmail());
        if (existingMember.isPresent()) {
            throw new RuntimeException();
        }

        String encodedPassword = passwordEncoder.encode(dto.getPassword());
        dto.setPassword(encodedPassword);
        dto.setAuthorities(authorities);
        dto.setActivated(true);
        return memberRepository.save(dto.toEntity());
    }

}