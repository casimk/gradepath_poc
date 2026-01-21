import { Injectable } from '@nestjs/common';
import { PassportStrategy } from '@nestjs/passport';
import { ConfigService } from '@nestjs/config';
import { Strategy } from 'passport-apple';

@Injectable()
export class AppleOAuthStrategy extends PassportStrategy(Strategy, 'apple') {
  constructor(private readonly configService: ConfigService) {
    super({
      clientID: configService.get('APPLE_CLIENT_ID') || '',
      teamID: configService.get('APPLE_TEAM_ID') || '',
      keyID: configService.get('APPLE_KEY_ID') || '',
      privateKeyLocation: configService.get('APPLE_PRIVATE_KEY_LOCATION') || '',
      callbackURL: configService.get('APPLE_CALLBACK_URL') || 'http://localhost:3000/auth/oauth/apple/callback',
      scope: ['email', 'name'],
    });
  }

  async validate(accessToken: string, refreshToken: string, profile: any, done: Function): Promise<void> {
    const user = {
      provider: 'apple',
      providerUserId: profile.id,
      email: profile.email,
      displayName: profile.displayName || profile.name?.firstName && profile.name?.lastName
        ? `${profile.name.firstName} ${profile.name.lastName}`
        : undefined,
      firstName: profile.name?.firstName,
      lastName: profile.name?.lastName,
      profileData: profile,
    };

    done(null, user);
  }
}
