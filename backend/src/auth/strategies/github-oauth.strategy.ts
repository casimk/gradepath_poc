import { Injectable } from '@nestjs/common';
import { PassportStrategy } from '@nestjs/passport';
import { ConfigService } from '@nestjs/config';
import { Strategy } from 'passport-github2';

@Injectable()
export class GitHubOAuthStrategy extends PassportStrategy(Strategy, 'github') {
  constructor(private readonly configService: ConfigService) {
    super({
      clientID: configService.get('GITHUB_CLIENT_ID') || '',
      clientSecret: configService.get('GITHUB_CLIENT_SECRET') || '',
      callbackURL: configService.get('GITHUB_CALLBACK_URL') || 'http://localhost:3000/auth/oauth/github/callback',
      scope: ['user:email'],
    });
  }

  async validate(accessToken: string, refreshToken: string, profile: any, done: (error: any, user?: any) => void): Promise<void> {
    const user = {
      provider: 'github',
      providerUserId: profile.id,
      email: profile.emails?.[0]?.value,
      displayName: profile.username || profile.displayName,
      username: profile.username,
      bio: profile.bio,
      location: profile.location,
      picture: profile.photos?.[0]?.value,
      profileData: profile,
    };

    done(null, user);
  }
}
